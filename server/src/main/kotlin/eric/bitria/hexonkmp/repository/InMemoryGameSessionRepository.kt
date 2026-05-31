package eric.bitria.hexonkmp.repository

import eric.bitria.hexonkmp.session.GameSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryGameSessionRepository : GameSessionRepository {
    private val sessions = ConcurrentHashMap<String, GameSession>()
    private val mutex = Mutex()

    // The single session currently accepting players; null when full or none exists yet.
    private var openSession: GameSession? = null

    override suspend fun findOrJoin(playerId: String): GameSession = mutex.withLock {
        val session = openSession?.takeIf { it.hasAvailableSlot() }
            ?: GameSession(UUID.randomUUID().toString()).also { sessions[it.gameId] = it }

        session.reserveSlot(playerId)

        openSession = if (session.hasAvailableSlot()) session else null
        session
    }

    override fun get(gameId: String): GameSession? = sessions[gameId]
}
