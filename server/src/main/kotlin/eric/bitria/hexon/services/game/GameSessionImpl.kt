package eric.bitria.hexon.services.game

import com.github.f4b6a3.uuid.UuidCreator
import eric.bitria.hexon.services.game.engine.GameEngine
import io.ktor.websocket.DefaultWebSocketSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class GameSessionImpl(
    private val repository: GameSessionRepository,
    override val sessionId: String = UuidCreator.getTimeBasedWithRandom().toString(),
    override val mode: String,
    override val maxPlayers: Int
) : GameSession {

    private val connectedPlayers = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private val reservedPlayers = ConcurrentHashMap<String, Long>()
    private val mutex = Mutex()
    private var isStarted = false

    override fun connectedPlayers(): Set<String> = connectedPlayers.keys

    override fun reservedPlayers(): Set<String> = reservedPlayers.keys

    override suspend fun reserveSlot(userId: String, timeoutMs: Long): Boolean = mutex.withLock {
        cleanupExpiredSlots(timeoutMs)
        if (connectedPlayers.containsKey(userId) || reservedPlayers.containsKey(userId)) {
            return true
        }
        if (connectedPlayers.size + reservedPlayers.size < maxPlayers) {
            reservedPlayers[userId] = System.currentTimeMillis()
            return true
        }
        return false
    }

    override suspend fun connectPlayer(userId: String, ws: DefaultWebSocketSession): Boolean = mutex.withLock {
        if (reservedPlayers.containsKey(userId)) {
            reservedPlayers.remove(userId)
            connectedPlayers[userId] = ws
            return true
        }
        return false
    }

    override suspend fun removePlayer(userId: String) {
        mutex.withLock {
            connectedPlayers.remove(userId)
            reservedPlayers.remove(userId)

            if (!isStarted && connectedPlayers.size + reservedPlayers.size < maxPlayers) {
                repository.returnSessionToQueue(mode, this)
            }
        }
    }

    override fun isReady(): Boolean {
        return connectedPlayers.size == maxPlayers
    }

    override suspend fun cleanupExpiredSlots(timeoutMs: Long) {
        val now = System.currentTimeMillis()
        val iterator = reservedPlayers.entries.iterator()
        var freedSlot = false
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value + timeoutMs < now) {
                iterator.remove()
                freedSlot = true
            }
        }
        if (freedSlot && !isStarted) {
            repository.returnSessionToQueue(mode, this)
        }
    }

    override suspend fun invitePlayer(userId: String): Boolean {
        return reserveSlot(userId, timeoutMs = 30000L)
    }

    override fun startGame(): GameEngine? {
        isStarted = true
        // Implementation for starting the game engine will go here
        return null
    }
}
