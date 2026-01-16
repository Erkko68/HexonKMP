package eric.bitria.hexon.services.game

import com.github.f4b6a3.uuid.UuidCreator
import eric.bitria.hexon.dtos.matchmaking.GameMessage
import eric.bitria.hexon.services.game.engine.BasicGameEngine
import eric.bitria.hexon.services.game.engine.GameEngine
import eric.bitria.hexon.services.game.engine.GameMessageSender
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class GameSessionImpl(
    private val repository: GameSessionRepository,
    private val mode: String,
    private val maxPlayers: Int,
    override val sessionId: String = UuidCreator.getTimeBasedWithRandom().toString()
) : GameSession, GameMessageSender {

    // --- State ---
    @Volatile override var isGameStarted: Boolean = false
        private set

    private var engine: GameEngine? = null

    private val connectedPlayers = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private val reservedSlots = ConcurrentHashMap<String, Long>()

    private val stateMutex = Mutex()

    // --- Matchmaking Phase ---

    override suspend fun reserveSlot(userId: String): Boolean {
        pruneExpiredSlots()
        // Fast approximate check before locking
        if (connectedPlayers.size + reservedSlots.size >= maxPlayers) return false

        // Clean expired slots implicitly or explicitly here
        // For simplicity, we just add the reservation
        reservedSlots[userId] = System.currentTimeMillis()
        return true
    }

    override fun hasAvailableSlots(): Boolean {
        pruneExpiredSlots()
        return (connectedPlayers.size + reservedSlots.size) < maxPlayers
    }

    // --- Connection Phase ---

    override suspend fun connectPlayer(userId: String, session: DefaultWebSocketSession): Boolean {
        stateMutex.withLock {
            // 1. Validation (Must have reservation or be reconnecting)
            if (!reservedSlots.containsKey(userId) && !connectedPlayers.containsKey(userId)) {
                return false
            }

            // 2. Upgrade State
            reservedSlots.remove(userId)
            connectedPlayers[userId] = session

            // 3. Logic Branch
            if (isGameStarted) {
                // Reconnection: Engine handles state sync
                engine?.onPlayerRejoin(userId)
            } else {
                // Lobby Phase
                if (connectedPlayers.size == maxPlayers) {
                    // FULL HOUSE: Launch the Engine
                    launchGameEngine()
                } else {
                    // STILL WAITING: Notify others "X/4 Players"
                    broadcastLobbyStatus()
                }
            }
            return true
        }
    }

    override suspend fun removePlayer(userId: String) {
        // Note: We don't remove from 'connectedPlayers' immediately inside the lock
        // if the game is started to allow for network blips/reconnects.

        stateMutex.withLock {
            if (isGameStarted) {
                // Game in progress: Tell engine
                engine?.onPlayerLeave(userId)
                // Optionally remove socket if you want to force re-handshake
                connectedPlayers.remove(userId)
            } else {
                // Lobby Phase: Full cleanup
                connectedPlayers.remove(userId)
                reservedSlots.remove(userId)

                // Return to queue since a spot opened up
                repository.addSession(mode, this) // Assuming 'addSession' acts as 'returnToQueue'

                broadcastLobbyStatus()
            }
        }
    }

    // --- Input Handling ---

    override suspend fun handleIncomingMessage(userId: String, message: GameMessage) {
        if (isGameStarted) {
            engine?.onMessage(userId, message)
        } else {
            // Optional: Handle Lobby Chat here
        }
    }

    // --- Output Handling (GameMessageSender) ---

    override suspend fun sendToPlayer(userId: String, message: GameMessage) {
        val session = connectedPlayers[userId] ?: return
        try {
            val json = Json.encodeToString(message)
            session.send(Frame.Text(json))
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun broadcast(message: GameMessage) {
        // No Lock needed: ConcurrentHashMap iterator is weakly consistent
        val json = Json.encodeToString(message)
        val frame = Frame.Text(json)

        connectedPlayers.values.forEach { session ->
            try {
                session.send(frame)
            } catch (e: Exception) {
                // Handle dead socket
            }
        }
    }

    // --- Helpers ---

    private fun pruneExpiredSlots() {
        val timeoutMs = 10_000L // e.g. 10 Seconds
        val now = System.currentTimeMillis()

        val removed = reservedSlots.entries.removeIf { (userId, timestamp) ->
            now - timestamp > timeoutMs
        }
    }

    private suspend fun launchGameEngine() {
        isGameStarted = true
        // Factory logic for different modes
        val newEngine = when (mode) {
            "classic" -> BasicGameEngine(sessionId)
            else -> BasicGameEngine(sessionId) // Default or Error
        }

        this.engine = newEngine

        // Start the engine logic (it will broadcast GAME_START)
        newEngine.start(connectedPlayers.keys.toList(), this)
    }

    private suspend fun broadcastLobbyStatus() {
        val msg = GameMessage.LobbyUpdate(
            players = connectedPlayers.size,
            maxPlayers = maxPlayers,
            isReady = false
        )
        broadcast(msg)
    }
}