package eric.bitria.hexon.services.game.session

import com.github.f4b6a3.uuid.UuidCreator
import eric.bitria.hexon.services.game.engine.GameEngine
import eric.bitria.hexon.services.game.engine.GameEngineImpl
import eric.bitria.hexon.services.game.engine.GameMessageSender
import eric.bitria.hexon.ws.GameMessage
import eric.bitria.hexon.ws.GameplayMessage
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
 * Matchmaking game session for quick-match games.
 * Lifecycle: WAITING → STARTING → RUNNING → DISPOSED
 * - Auto-readies all players
 * - Auto-starts when full
 * - Disposes after game ends
 */
class MatchmakingGameSession(
    private val mode: GameMode,
    private val maxPlayers: Int,
    override val sessionId: String = UuidCreator.getTimeBasedWithRandom().toString()
) : BaseGameSession, GameMessageSender {

    private val logger = LoggerFactory.getLogger(MatchmakingGameSession::class.java)

    // ==================== State Machine ====================

    private enum class State {
        WAITING,         // Waiting for players to fill slots
        STARTING,        // GameStarted sent, waiting for ReadyForGame from all clients
        RUNNING,         // Game engine is active
        DISPOSED         // Game ended, session should be removed
    }

    @Volatile private var currentState = State.WAITING
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
        return currentState == State.WAITING &&
               (connectedPlayers.size + reservedSlots.size) < maxPlayers
    }

    override suspend fun reserveSlot(userId: String): Boolean {
        if (currentState != State.WAITING) return false

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
            State.WAITING -> handlePlayerJoin(userId, username)
            State.STARTING, State.RUNNING -> handlePlayerReconnect(userId)
            State.DISPOSED -> return false
        }

        return true
    }

    override suspend fun removePlayer(userId: String) {
        when (currentState) {
            State.WAITING -> removePlayerFromLobby(userId)
            State.STARTING, State.RUNNING -> handlePlayerDisconnect(userId)
            State.DISPOSED -> {} // Already disposed
        }
    }

    override suspend fun handleIncomingMessage(userId: String, message: GameMessage) {
        when (message) {
            is LobbyIntent.ReadyForGame -> handleReadyForGame(userId)
            is LobbyIntent.LeaveLobby -> removePlayer(userId)
            is GameplayMessage -> handleGameplayMessage(userId, message)
            else -> logger.warn("Unknown message type from $userId: ${message::class.simpleName}")
        }
    }

    override fun setLifecycleListener(listener: SessionLifecycleListener) {
        this.lifecycleListener = listener
    }

    // ==================== Player Join Phase ====================

    private suspend fun handlePlayerJoin(userId: String, username: String) {
        val newPlayer = LobbyPlayer(
            id = userId,
            name = username,
            color = assignAvailableColor(),
            isReady = true,  // Auto-ready in matchmaking
            isHost = false
        )

        lobbyPlayers[userId] = newPlayer
        logger.info("Player $username joined matchmaking session $sessionId (${connectedPlayers.size}/$maxPlayers)")

        // Send snapshot to the new player
        sendToPlayer(userId, LobbyEvent.LobbySnapshot(
            lobbyId = sessionId,
            lobbyPlayers = lobbyPlayers.values.toList(),
            maxPlayers = maxPlayers,
            availableColors = availableColors
        ))

        // Notify others
        broadcast(LobbyEvent.PlayerJoined(newPlayer), excludeUserId = userId)

        // Auto-start when full
        if (connectedPlayers.size == maxPlayers) {
            startGame()
        }
    }

    private suspend fun removePlayerFromLobby(userId: String) {
        val session = connectedPlayers.remove(userId)
        val removedPlayer = lobbyPlayers.remove(userId)
        reservedSlots.remove(userId)

        if (removedPlayer != null) {
            logger.info("Player $userId left matchmaking session $sessionId")
            broadcast(LobbyEvent.PlayerLeft(userId))
        }

        session?.closeGracefully("Left Lobby")

        // Check if empty
        if (connectedPlayers.isEmpty()) {
            lifecycleListener?.onSessionEmpty(sessionId)
        }
    }

    // ==================== Game Start Phase ====================

    private suspend fun startGame() {
        stateMutex.withLock {
            if (currentState != State.WAITING) return
            currentState = State.STARTING
        }

        // Generate unique game ID
        currentGameId = UuidCreator.getTimeBasedWithRandom().toString()

        // Notify lifecycle listener
        lifecycleListener?.onGameStarting(sessionId, currentGameId!!)

        logger.info("Matchmaking game $currentGameId starting in session $sessionId, waiting for ${connectedPlayers.size} players to signal ready")

        // Notify all clients - they will respond with ReadyForGame
        broadcast(LobbyEvent.GameStarted)
    }

    private suspend fun handleReadyForGame(userId: String) {
        if (currentState != State.STARTING) return

        playersReadyForGame.add(userId)
        logger.debug("Player $userId ready for game (${playersReadyForGame.size}/${connectedPlayers.size})")

        // Check if all players are ready
        if (playersReadyForGame.containsAll(connectedPlayers.keys)) {
            initializeGame()
        }
    }

    private suspend fun initializeGame() {
        stateMutex.withLock {
            if (currentState != State.STARTING) return
            currentState = State.RUNNING
        }

        logger.info("All players ready, initializing matchmaking game $currentGameId")

        val gameId = currentGameId ?: return
        val players = lobbyPlayers.values.map { it.copy() }

        engine = GameEngineImpl(gameId)
        engine?.start(gameId, players, this)
    }

    // ==================== Game Running Phase ====================

    private suspend fun handleGameplayMessage(userId: String, message: GameplayMessage) {
        if (currentState != State.RUNNING) return
        engine?.onMessage(userId, message)
    }

    private suspend fun handlePlayerReconnect(userId: String) {
        logger.info("Player $userId reconnecting to game $currentGameId")
        engine?.onPlayerRejoin(userId)
    }

    private suspend fun handlePlayerDisconnect(userId: String) {
        if (connectedPlayers.remove(userId) != null) {
            logger.info("Player $userId disconnected from game $currentGameId")
            engine?.onPlayerLeave(userId)
        }
    }

    // ==================== Game End Phase ====================

    override suspend fun onGameEnded(winnerId: String?) {
        stateMutex.withLock {
            if (currentState == State.DISPOSED) return
            currentState = State.DISPOSED
        }

        logger.info("Matchmaking game $currentGameId ended, disposing session $sessionId")

        val gameId = currentGameId
        if (gameId != null) {
            lifecycleListener?.onGameEnded(sessionId, gameId)
        }

        // Close all WebSocket connections gracefully
        connectedPlayers.forEach { (userId, session) ->
            try {
                session.close(CloseReason(CloseReason.Codes.NORMAL, "Game ended"))
            } catch (e: Exception) {
                logger.error("Failed to close connection for $userId: ${e.message}")
            }
        }

        // Clear session state
        connectedPlayers.clear()
        lobbyPlayers.clear()
        reservedSlots.clear()
        playersReadyForGame.clear()
        engine = null

        // Notify repository to remove this session
        lifecycleListener?.onSessionEmpty(sessionId)
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

    private suspend fun DefaultWebSocketSession.closeGracefully(reason: String) {
        try {
            close(CloseReason(CloseReason.Codes.NORMAL, reason))
        } catch (_: Exception) {
            // Socket might already be closed
        }
    }
}

