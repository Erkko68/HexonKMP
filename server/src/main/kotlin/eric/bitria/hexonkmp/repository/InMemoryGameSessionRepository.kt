package eric.bitria.hexonkmp.repository

import eric.bitria.hexonkmp.session.GameSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryGameSessionRepository : GameSessionRepository {
    private val sessions = ConcurrentHashMap<String, GameSession>()
    private val mutex = Mutex()
    private var openSession: GameSession? = null

    override suspend fun findOrJoin(playerId: String): GameSession = mutex.withLock {
        val session = openSession?.takeIf { it.hasAvailableSlot() }
            ?: GameSession(gameId = UUID.randomUUID().toString(), onEmpty = ::removeSession)
                .also { sessions[it.gameId] = it }

        session.reserveSlot(playerId)
        openSession = if (session.hasAvailableSlot()) session else null
        session
    }

    override fun get(gameId: String): GameSession? = sessions[gameId]

    // Called by GameSession when all players have left.
    // Runs under the repository mutex so findOrJoin can never hand out a dead session.
    private suspend fun removeSession(gameId: String) = mutex.withLock {
        sessions.remove(gameId)
        if (openSession?.gameId == gameId) openSession = null
    }
}
