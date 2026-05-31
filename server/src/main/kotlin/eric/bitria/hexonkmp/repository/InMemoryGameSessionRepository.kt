package eric.bitria.hexonkmp.repository

import eric.bitria.hexonkmp.session.GameSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryGameSessionRepository : GameSessionRepository {
    private val sessions = ConcurrentHashMap<String, GameSession>()
    private val playerIndex = ConcurrentHashMap<String, String>() // playerId → gameId
    private val mutex = Mutex()
    private var openSession: GameSession? = null

    override suspend fun findOrJoin(playerId: String): GameSession = mutex.withLock {
        // Reconnect: player already has a live session
        playerIndex[playerId]?.let { gameId -> sessions[gameId] }
            ?.also { return@withLock it }

        // New slot: find the open session or create one
        val session = openSession?.takeIf { it.hasAvailableSlot() }
            ?: GameSession(gameId = UUID.randomUUID().toString(), onEmpty = ::removeSession)
                .also { sessions[it.gameId] = it }

        session.reserveSlot(playerId)
        playerIndex[playerId] = session.gameId
        openSession = if (session.hasAvailableSlot()) session else null
        session
    }

    override fun get(gameId: String): GameSession? = sessions[gameId]

    private suspend fun removeSession(gameId: String) = mutex.withLock {
        sessions.remove(gameId)
        if (openSession?.gameId == gameId) openSession = null
        playerIndex.entries.removeIf { it.value == gameId }
    }
}
