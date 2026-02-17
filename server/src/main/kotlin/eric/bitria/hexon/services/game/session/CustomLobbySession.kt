package eric.bitria.hexon.services.game.session

import com.github.f4b6a3.uuid.UuidCreator
import eric.bitria.hexon.services.game.engine.GameEngine
import eric.bitria.hexon.services.game.engine.GameEngineImpl
import eric.bitria.hexon.services.game.engine.GameMessageSender
import eric.bitria.hexon.ws.GameMessage
import eric.bitria.hexon.ws.GameplayMessage
import eric.bitria.hexon.ws.LobbyErrorCode
import eric.bitria.hexon.ws.LobbyEvent
import eric.bitria.hexon.ws.LobbyIntent
import eric.bitria.hexon.ws.lobby.GameMode
import eric.bitria.hexon.ws.lobby.LobbyPlayer
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Custom lobby session for player-hosted games.
 * Lifecycle: LOBBY → GAME_STARTING → GAME_RUNNING → LOBBY (repeatable)
 * - Supports host controls (ready toggle, color selection, manual start)
 * - Returns to LOBBY after game ends for rematch
 * - Cleaned up only when all players leave
 */
class CustomLobbySession(
    private val mode: GameMode,
    private val maxPlayers: Int,
    override val sessionId: String = UuidCreator.getTimeBasedWithRandom().toString()
) : BaseGameSession, GameMessageSender {

    private val logger = LoggerFactory.getLogger(CustomLobbySession::class.java)

    // ==================== State Machine ====================

    private enum class State {
        LOBBY,           // Waiting for players and ready checks
        GAME_STARTING,   // GameStarted sent, waiting for ReadyForGame from all clients
        GAME_RUNNING     // Game engine is active
    }

    @Volatile private var currentState = State.LOBBY
    private val stateMutex = Mutex()

    // ==================== Collections ====================

    private val connectedPlayers = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private val lobbyPlayers = ConcurrentHashMap<String, LobbyPlayer>()
    private val reservedSlots = ConcurrentHashMap<String, Long>()
    private val playersReadyForGame = ConcurrentHashMap.newKeySet<String>()

    private var engine: GameEngine? = null
    private var currentGameId: String? = null
    private var lifecycleListener: SessionLifecycleListener? = null

    // ==================== Configuration ====================

    private val availableColors = listOf(
        "#0000FF", "#FF0000", "#FFFFFF",
        "#FFA500", "#008000", "#A52A2A"
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    // ==================== BaseGameSession Interface ====================

    override fun hasAvailableSlots(): Boolean {
        pruneExpiredSlots()
        return (connectedPlayers.size + reservedSlots.size) < maxPlayers
    }

    override suspend fun reserveSlot(userId: String): Boolean {
        pruneExpiredSlots()

        if (connectedPlayers.size + reservedSlots.size >= maxPlayers) {
            return false
        }

        reservedSlots[userId] = System.currentTimeMillis()
        return true
    }

    override suspend fun connectPlayer(
        userId: String,
        username: String,
        session: DefaultWebSocketSession
    ): Boolean {
        // Validate reservation exists
        if (!reservedSlots.containsKey(userId) && !connectedPlayers.containsKey(userId)) {
            logger.warn("Connection rejected for $userId - no reservation")
            return false
        }

        // Accept connection
        reservedSlots.remove(userId)
        connectedPlayers[userId] = session

        // Handle based on current state
        when (currentState) {
            State.LOBBY -> handlePlayerJoinLobby(userId, username)
            State.GAME_STARTING, State.GAME_RUNNING -> handlePlayerReconnect(userId)
        }

        return true
    }

    override suspend fun removePlayer(userId: String) {
        when (currentState) {
            State.LOBBY -> removePlayerFromLobby(userId)
            State.GAME_STARTING, State.GAME_RUNNING -> handlePlayerDisconnect(userId)
        }
    }

    override suspend fun handleIncomingMessage(userId: String, message: GameMessage) {
        when (message) {
            is LobbyIntent -> handleLobbyIntent(userId, message)
            is GameplayMessage -> handleGameplayMessage(userId, message)
            else -> logger.warn("Unknown message type from $userId: ${message::class.simpleName}")
        }
    }

    override fun setLifecycleListener(listener: SessionLifecycleListener) {
        this.lifecycleListener = listener
    }

    // ==================== Lobby Phase ====================

    private suspend fun handlePlayerJoinLobby(userId: String, username: String) {
        val newPlayer = LobbyPlayer(
            id = userId,
            name = username,
            color = assignAvailableColor(),
            isReady = false,  // Manual ready in custom lobbies
            isHost = lobbyPlayers.isEmpty()  // First player is host
        )

        lobbyPlayers[userId] = newPlayer
        logger.info("Player $username joined custom lobby $sessionId (${connectedPlayers.size}/$maxPlayers)")

        // Send snapshot to the new player
        sendToPlayer(userId, LobbyEvent.LobbySnapshot(
            lobbyId = sessionId,
            lobbyPlayers = lobbyPlayers.values.toList(),
            maxPlayers = maxPlayers,
            availableColors = availableColors
        ))

        // Notify others
        broadcast(LobbyEvent.PlayerJoined(newPlayer), excludeUserId = userId)
    }

    private suspend fun removePlayerFromLobby(userId: String) {
        val session = connectedPlayers.remove(userId)
        val removedPlayer = lobbyPlayers.remove(userId)
        reservedSlots.remove(userId)

        if (removedPlayer != null) {
            logger.info("Player $userId left custom lobby $sessionId")

            // If host left, assign new host
            if (removedPlayer.isHost && lobbyPlayers.isNotEmpty()) {
                val newHost = lobbyPlayers.values.first()
                val updated = newHost.copy(isHost = true)
                lobbyPlayers[newHost.id] = updated
                broadcast(LobbyEvent.PlayerUpdated(updated))
            }

            broadcast(LobbyEvent.PlayerLeft(userId))
        }

        session?.closeGracefully("Left Lobby")

        // Check if lobby is empty
        if (connectedPlayers.isEmpty()) {
            logger.info("Custom lobby $sessionId is now empty, notifying for cleanup")
            lifecycleListener?.onSessionEmpty(sessionId)
        }
    }

    private suspend fun handleLobbyIntent(userId: String, intent: LobbyIntent) {
        when (intent) {
            is LobbyIntent.ReadyForGame -> handleReadyForGame(userId)
            is LobbyIntent.LeaveLobby -> removePlayer(userId)

            is LobbyIntent.ChangeColor -> {
                if (currentState != State.LOBBY) return

                val player = lobbyPlayers[userId] ?: return
                if (isColorAvailable(intent.newColor)) {
                    val updated = player.copy(color = intent.newColor)
                    lobbyPlayers[userId] = updated
                    broadcast(LobbyEvent.PlayerUpdated(updated))
                } else {
                    sendToPlayer(userId, LobbyEvent.LobbyError("Color already taken", LobbyErrorCode.COLOR_TAKEN))
                }
            }

            is LobbyIntent.ToggleReady -> {
                if (currentState != State.LOBBY) return

                val player = lobbyPlayers[userId] ?: return
                val updated = player.copy(isReady = intent.isReady)
                lobbyPlayers[userId] = updated
                broadcast(LobbyEvent.PlayerUpdated(updated))
            }

            is LobbyIntent.RequestStartGame -> {
                if (currentState != State.LOBBY) return

                val player = lobbyPlayers[userId] ?: return
                if (player.isHost && allPlayersReady()) {
                    startGame()
                } else if (player.isHost && !allPlayersReady()) {
                    sendToPlayer(userId, LobbyEvent.LobbyError("Not all players are ready", LobbyErrorCode.NOT_ALL_READY))
                }
            }
        }
    }

    // ==================== Game Start Phase ====================

    private suspend fun startGame() {
        stateMutex.withLock {
            if (currentState != State.LOBBY) return
            currentState = State.GAME_STARTING
        }

        // Generate unique game ID for this instance
        currentGameId = UuidCreator.getTimeBasedWithRandom().toString()

        // Notify lifecycle listener
        lifecycleListener?.onGameStarting(sessionId, currentGameId!!)

        logger.info("Custom lobby game $currentGameId starting in session $sessionId, waiting for ${connectedPlayers.size} players to signal ready")

        // Notify all clients - they will respond with ReadyForGame
        broadcast(LobbyEvent.GameStarted)
    }

    private suspend fun handleReadyForGame(userId: String) {
        if (currentState != State.GAME_STARTING) return

        playersReadyForGame.add(userId)
        logger.debug("Player $userId ready for game (${playersReadyForGame.size}/${connectedPlayers.size})")

        // Check if all players are ready
        if (playersReadyForGame.containsAll(connectedPlayers.keys)) {
            initializeGame()
        }
    }

    private suspend fun initializeGame() {
        stateMutex.withLock {
            if (currentState != State.GAME_STARTING) return
            currentState = State.GAME_RUNNING
        }

        logger.info("All players ready, initializing custom lobby game $currentGameId")

        val gameId = currentGameId ?: return
        val players = lobbyPlayers.values.map { it.copy() }

        engine = GameEngineImpl(gameId)
        engine?.start(gameId, players, this)
    }

    // ==================== Game Running Phase ====================

    private suspend fun handleGameplayMessage(userId: String, message: GameplayMessage) {
        if (currentState != State.GAME_RUNNING) return
        engine?.onMessage(userId, message)
    }

    private suspend fun handlePlayerReconnect(userId: String) {
        logger.info("Player $userId reconnecting to game $currentGameId in lobby $sessionId")
        engine?.onPlayerRejoin(userId)
    }

    private suspend fun handlePlayerDisconnect(userId: String) {
        if (connectedPlayers.remove(userId) != null) {
            logger.info("Player $userId disconnected from game $currentGameId")
            engine?.onPlayerLeave(userId)

            // If all players disconnect during game, clean up
            if (connectedPlayers.isEmpty()) {
                logger.info("All players disconnected from custom lobby $sessionId during game, notifying for cleanup")
                lifecycleListener?.onSessionEmpty(sessionId)
            }
        }
    }

    // ==================== Game End Phase (Return to Lobby) ====================

    override suspend fun onGameEnded(winnerId: String?) {
        stateMutex.withLock {
            if (currentState != State.GAME_RUNNING) return
            currentState = State.LOBBY
        }

        logger.info("Custom lobby game $currentGameId ended, returning to lobby $sessionId")

        val gameId = currentGameId
        if (gameId != null) {
            lifecycleListener?.onGameEnded(sessionId, gameId)
        }

        // Reset game state but keep lobby
        playersReadyForGame.clear()
        engine = null
        currentGameId = null

        // Reset all players to not ready
        lobbyPlayers.forEach { (userId, player) ->
            val updated = player.copy(isReady = false)
            lobbyPlayers[userId] = updated
        }

        // Notify players that game ended and they're back in lobby
        broadcast(LobbyEvent.LobbySnapshot(
            lobbyId = sessionId,
            lobbyPlayers = lobbyPlayers.values.toList(),
            maxPlayers = maxPlayers,
            availableColors = availableColors
        ))

        logger.info("Custom lobby $sessionId ready for next game")
    }

    // ==================== GameMessageSender Interface ====================

    override suspend fun sendToPlayer(receiverId: String, message: GameMessage) {
        val session = connectedPlayers[receiverId] ?: return

        try {
            val jsonText = json.encodeToString(message)
            session.send(Frame.Text(jsonText))
        } catch (e: Exception) {
            logger.error("Failed to send ${message::class.simpleName} to $receiverId: ${e.message}")
        }
    }

    override suspend fun broadcast(message: GameMessage) {
        broadcast(message, excludeUserId = null)
    }

    override suspend fun broadcast(message: GameMessage, excludeUserId: String?) {
        val jsonText = json.encodeToString(message)

        coroutineScope {
            connectedPlayers.forEach { (userId, session) ->
                if (userId != excludeUserId) {
                    launch {
                        try {
                            session.send(Frame.Text(jsonText))
                        } catch (e: Exception) {
                            logger.error("Failed to broadcast ${message::class.simpleName} to $userId: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    // ==================== Helpers ====================

    private fun pruneExpiredSlots() {
        val now = System.currentTimeMillis()
        val timeout = 10_000L
        reservedSlots.entries.removeIf { (_, timestamp) -> now - timestamp > timeout }
    }

    private fun assignAvailableColor(): String {
        val usedColors = lobbyPlayers.values.map { it.color }.toSet()
        return availableColors.firstOrNull { it !in usedColors } ?: "#808080"
    }

    private fun isColorAvailable(color: String): Boolean {
        return lobbyPlayers.values.none { it.color == color }
    }

    private fun allPlayersReady(): Boolean {
        return lobbyPlayers.values.all { it.isReady }
    }

    private suspend fun DefaultWebSocketSession.closeGracefully(reason: String) {
        try {
            close(CloseReason(CloseReason.Codes.NORMAL, reason))
        } catch (_: Exception) {
            // Socket might already be closed
        }
    }
}

