package eric.bitria.hexon.services.game

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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages a single game session through its lifecycle:
 * 1. LOBBY: Players connect, configure settings
 * 2. GAME_STARTING: GameStarted broadcast, waiting for all ReadyForGame signals
 * 3. GAME_RUNNING: Game engine handles gameplay
 */
class GameSessionImpl(
    private val isCustom: Boolean = false,
    private val mode: GameMode,
    private val maxPlayers: Int,
    override val sessionId: String = UuidCreator.getTimeBasedWithRandom().toString()
) : GameSession, GameMessageSender {

    private val logger = LoggerFactory.getLogger(GameSessionImpl::class.java)

    // ==================== State Machine ====================

    private enum class State {
        LOBBY,           // Waiting for players
        GAME_STARTING,   // GameStarted sent, waiting for ReadyForGame from all clients
        GAME_RUNNING     // Engine is active, game in progress
    }

    @Volatile private var currentState = State.LOBBY
    private val stateMutex = Mutex()

    // ==================== Collections ====================

    // Thread-safe maps for concurrent WebSocket access
    private val connectedPlayers = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private val lobbyPlayers = ConcurrentHashMap<String, LobbyPlayer>()
    private val reservedSlots = ConcurrentHashMap<String, Long>()
    private val playersReadyForGame = ConcurrentHashMap.newKeySet<String>()

    private var engine: GameEngine? = null

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

    // ==================== GameSession Interface ====================

    override val isGameStarted: Boolean
        get() = currentState != State.LOBBY

    override suspend fun reserveSlot(userId: String): Boolean {
        pruneExpiredSlots()

        if (connectedPlayers.size + reservedSlots.size >= maxPlayers) {
            return false
        }

        reservedSlots[userId] = System.currentTimeMillis()
        return true
    }

    override fun hasAvailableSlots(): Boolean {
        pruneExpiredSlots()
        return (connectedPlayers.size + reservedSlots.size) < maxPlayers
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

    // ==================== Game Start Phase ====================

    private suspend fun startGame() {
        stateMutex.withLock {
            if (currentState != State.LOBBY) return
            currentState = State.GAME_STARTING
        }

        // Create engine (but don't initialize yet)
        engine = GameEngineImpl(sessionId)

        logger.info("Game $sessionId starting, waiting for ${connectedPlayers.size} players to signal ready")

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

        logger.info("All players ready, initializing game $sessionId")

        val currentEngine = engine ?: return
        val players = lobbyPlayers.values.map { it.copy() }

        currentEngine.start(players, this)
    }

    // ==================== Game Running Phase ====================

    private suspend fun handleGameplayMessage(userId: String, message: GameplayMessage) {
        if (currentState != State.GAME_RUNNING) return
        engine?.onMessage(userId, message)
    }

    private suspend fun handlePlayerReconnect(userId: String) {
        logger.info("Player $userId reconnecting to game $sessionId")
        engine?.onPlayerRejoin(userId)
    }

    private suspend fun handlePlayerDisconnect(userId: String) {
        logger.info("Player $userId disconnected from game $sessionId")
        engine?.onPlayerLeave(userId)
        connectedPlayers.remove(userId)
    }

    // ==================== GameMessageSender Interface ====================

    private suspend fun handlePlayerJoinLobby(userId: String, username: String) {
        val newPlayer = LobbyPlayer(
            id = userId,
            name = username,
            color = assignAvailableColor(),
            isReady = !isCustom,  // Auto-ready in matchmaking mode
            isHost = isCustom && lobbyPlayers.isEmpty()
        )

        lobbyPlayers[userId] = newPlayer
        logger.info("Player $username joined lobby $sessionId (${connectedPlayers.size}/$maxPlayers)")

        // Send snapshot to the new player
        sendToPlayer(userId, LobbyEvent.LobbySnapshot(
            lobbyId = sessionId,
            lobbyPlayers = lobbyPlayers.values.toList(),
            maxPlayers = maxPlayers,
            availableColors = availableColors
        ))

        // Notify others
        broadcast(LobbyEvent.PlayerJoined(newPlayer), excludeUserId = userId)

        // Auto-start in matchmaking mode when full
        if (!isCustom && connectedPlayers.size == maxPlayers) {
            startGame()
        }
    }

    private suspend fun removePlayerFromLobby(userId: String) {
        val session = connectedPlayers.remove(userId)
        lobbyPlayers.remove(userId)
        reservedSlots.remove(userId)

        logger.info("Player $userId left lobby $sessionId")
        broadcast(LobbyEvent.PlayerLeft(userId))

        session?.closeGracefully("Left Lobby")
    }

    private suspend fun handleLobbyIntent(userId: String, intent: LobbyIntent) {
        when (intent) {
            is LobbyIntent.ReadyForGame -> handleReadyForGame(userId)
            is LobbyIntent.LeaveLobby -> removePlayer(userId)

            is LobbyIntent.ChangeColor -> {
                if (!isCustom || currentState != State.LOBBY) return

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
                if (!isCustom || currentState != State.LOBBY) return

                val player = lobbyPlayers[userId] ?: return
                val updated = player.copy(isReady = intent.isReady)
                lobbyPlayers[userId] = updated
                broadcast(LobbyEvent.PlayerUpdated(updated))
            }

            is LobbyIntent.RequestStartGame -> {
                if (!isCustom || currentState != State.LOBBY) return

                val player = lobbyPlayers[userId] ?: return
                if (player.isHost) {
                    startGame()
                }
            }
        }
    }

    override suspend fun sendToPlayer(playerId: String, message: GameMessage) {
        val session = connectedPlayers[playerId] ?: return

        try {
            val jsonText = json.encodeToString(message)
            session.send(Frame.Text(jsonText))
        } catch (e: Exception) {
            logger.error("Failed to send ${message::class.simpleName} to $playerId: ${e.message}")
        }
    }

    override suspend fun broadcast(message: GameMessage) {
        broadcast(message, excludeUserId = null)
    }

    override suspend fun broadcast(message: GameMessage, excludeUserId: String?) {
        val jsonText = json.encodeToString(message)
        val frame = Frame.Text(jsonText)

        connectedPlayers.forEach { (userId, session) ->
            if (userId != excludeUserId) {
                try {
                    session.send(frame)
                } catch (e: Exception) {
                    logger.error("Failed to broadcast ${message::class.simpleName} to $userId: ${e.message}")
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

    private suspend fun DefaultWebSocketSession.closeGracefully(reason: String) {
        try {
            close(CloseReason(CloseReason.Codes.NORMAL, reason))
        } catch (_: Exception) {
            // Socket might already be closed
        }
    }
}