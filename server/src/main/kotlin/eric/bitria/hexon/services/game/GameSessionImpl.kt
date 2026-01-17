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
import eric.bitria.hexon.ws.data.Player
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class GameSessionImpl(
    private val isCustom: Boolean = false,
    private val mode: String,
    private val maxPlayers: Int,
    override val sessionId: String = UuidCreator.getTimeBasedWithRandom().toString()
) : GameSession, GameMessageSender {

    // --- State ---
    @Volatile override var isGameStarted: Boolean = false
        private set

    private var engine: GameEngine? = null

    private val connectedPlayers = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private val players = ConcurrentHashMap<String, Player>()
    private val reservedSlots = ConcurrentHashMap<String, Long>()
    private val allColors = listOf("Red", "Blue", "White", "Orange", "Green", "Brown")

    private val stateMutex = Mutex()

    // --- Matchmaking Phase ---
    override suspend fun reserveSlot(userId: String): Boolean {
        stateMutex.withLock {
            pruneExpiredSlots()

            if (connectedPlayers.size + reservedSlots.size >= maxPlayers) {
                return false
            }

            reservedSlots[userId] = System.currentTimeMillis()
            return true
        }
    }

    override fun hasAvailableSlots(): Boolean {
        pruneExpiredSlots()
        return (connectedPlayers.size + reservedSlots.size) < maxPlayers
    }

    // --- Connection Phase ---

    override suspend fun connectPlayer(userId: String, username: String, session: DefaultWebSocketSession): Boolean {
        stateMutex.withLock {
            // 1. Validation
            if (!reservedSlots.containsKey(userId) && !connectedPlayers.containsKey(userId)) {
                return false
            }

            // 2. Accept Connection
            reservedSlots.remove(userId)
            connectedPlayers[userId] = session

            // 3. Logic Branch
            if (isGameStarted) {
                // Reconnection
                engine?.onPlayerRejoin(userId)
            } else {
                // --- LOBBY LOGIC ---
                val isFirstPlayer = players.isEmpty()
                val initialReady = !isCustom
                val isHost = if (isCustom) isFirstPlayer else false

                val newPlayer = Player(
                    id = userId,
                    name = username,
                    color = assignAvailableColor(),
                    isReady = initialReady,
                    isHost = isHost
                )

                players[userId] = newPlayer

                val snapshot = LobbyEvent.LobbySnapshot(
                    lobbyId = sessionId,
                    players = players.values.toList(),
                    maxPlayers = maxPlayers,
                    availableColors = allColors
                )
                sendToPlayer(userId, snapshot)
                broadcast(LobbyEvent.PlayerJoined(newPlayer), excludeUserId = userId)

                if (!isCustom && connectedPlayers.size == maxPlayers) {
                    launchGameEngine()
                }
            }
            return true
        }
    }

    override suspend fun removePlayer(userId: String) {
        stateMutex.withLock {
            removePlayerInternal(userId)
        }
    }

    /**
     * Internal helper that contains the actual logic.
     * MUST be called from within a `stateMutex.withLock` block.
     */
    private suspend fun removePlayerInternal(userId: String) {
        // Idempotency check: If player is already gone, do nothing.
        // This prevents double-broadcasting if 'LeaveLobby' is sent AND socket closes.
        if (!players.containsKey(userId) && !connectedPlayers.containsKey(userId)) {
            return
        }

        if (isGameStarted) {
            // Game in progress: Logic handled by Engine
            engine?.onPlayerLeave(userId)
            connectedPlayers.remove(userId)
        } else {
            // Lobby Phase: Full cleanup
            val session = connectedPlayers.remove(userId)
            players.remove(userId)
            reservedSlots.remove(userId)

            // Notify others
            broadcast(LobbyEvent.PlayerLeft(userId))

            try {
                session?.close(CloseReason(CloseReason.Codes.NORMAL, "Left Lobby"))
            } catch (e: Exception) {
                // Socket might already be closed
            }
        }
    }

    // --- Input Handling ---

    override suspend fun handleIncomingMessage(userId: String, message: GameMessage) {
        if (isGameStarted && message is GameplayMessage) {
            engine?.onMessage(userId, message)
            return
        }

        if (message is LobbyIntent) {
            stateMutex.withLock {

                val player = players[userId] ?: return

                when (message) {
                    is LobbyIntent.ChangeColor -> {
                        if (!isCustom) return
                        if (isColorAvailable(message.newColor)) {
                            val updated = player.copy(color = message.newColor)
                            players[userId] = updated
                            broadcast(LobbyEvent.PlayerUpdated(updated))
                        } else {
                            sendToPlayer(userId, LobbyEvent.LobbyError("Color already taken", LobbyErrorCode.COLOR_TAKEN))
                        }
                    }

                    is LobbyIntent.ToggleReady -> {
                        if (!isCustom) return
                        val updated = player.copy(isReady = message.isReady)
                        players[userId] = updated
                        broadcast(LobbyEvent.PlayerUpdated(updated))
                    }

                    is LobbyIntent.RequestStartGame -> {
                        if (isCustom && player.isHost) {
                            launchGameEngine()
                        }
                    }

                    is LobbyIntent.LeaveLobby -> {
                        // Safe call: We are already inside the lock,
                        // so we call the internal version directly.
                        removePlayerInternal(userId)
                    }
                }
            }
        }
    }

    // --- Output Handling ---
    override suspend fun sendToPlayer(userId: String, message: GameMessage) {
        val session = connectedPlayers[userId] ?: return
        try {
            val json = Json.encodeToString(message)
            session.send(Frame.Text(json))
        } catch (e: Exception) { /* Log */ }
    }

    override suspend fun broadcast(message: GameMessage) {
        broadcast(message, null)
    }

    suspend fun broadcast(message: GameMessage, excludeUserId: String? = null) {
        val json = Json.encodeToString(message)
        val frame = Frame.Text(json)
        connectedPlayers.forEach { (userId, session) ->
            if (userId != excludeUserId) {
                try { session.send(frame) } catch (e: Exception) { /* Dead socket */ }
            }
        }
    }

    // --- Helpers ---

    private fun pruneExpiredSlots() {
        val timeoutMs = 10_000L
        val now = System.currentTimeMillis()
        reservedSlots.entries.removeIf { (_, timestamp) -> now - timestamp > timeoutMs }
    }

    private fun assignAvailableColor(): String {
        val usedColors = players.values.map { it.color }.toSet()
        return allColors.firstOrNull { it !in usedColors } ?: "Gray"
    }

    private fun isColorAvailable(color: String): Boolean {
        return players.values.none { it.color == color }
    }

    private suspend fun launchGameEngine() {
        val newEngine = GameEngineImpl(sessionId)
        this.engine = newEngine
        isGameStarted = true

        // Startup logic
        val enginePlayers = players.values.map { lobbyPlayer ->
            Player(lobbyPlayer.id, lobbyPlayer.name, lobbyPlayer.color, lobbyPlayer.isReady, lobbyPlayer.isHost)
        }

        newEngine.start(enginePlayers, this)
    }
}