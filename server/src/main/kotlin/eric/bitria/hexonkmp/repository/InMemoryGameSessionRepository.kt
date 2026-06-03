package eric.bitria.hexonkmp.repository

import eric.bitria.hexonkmp.core.game.model.Redactable
import eric.bitria.hexonkmp.session.GameSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Game-agnostic matchmaking: it creates sessions through an injected factory
// (which knows the concrete game — its engine, codec, and match config) and never
// names a game type itself.
class InMemoryGameSessionRepository<S : Redactable<S>, A, E : Redactable<E>>(
    // (gameId, onEmpty) -> a fresh session for this game. onEmpty lets the session
    // tell the repository to clean up when its last player leaves.
    private val newSession: (gameId: String, onEmpty: suspend (gameId: String) -> Unit) -> GameSession<S, A, E>,
) : GameSessionRepository<S, A, E> {
    private val sessions = ConcurrentHashMap<String, GameSession<S, A, E>>()
    private val playerIndex = ConcurrentHashMap<String, String>() // playerId → gameId
    private val mutex = Mutex()
    private var openSession: GameSession<S, A, E>? = null

    override suspend fun findOrJoin(playerId: String): GameSession<S, A, E> = mutex.withLock {
        // Reconnect: player already mapped to a session that still exists — put
        // them back into it, re-reserving the slot freed when they disconnected.
        playerIndex[playerId]?.let { gameId -> sessions[gameId] }
            ?.also {
                it.reserveSlot(playerId)
                return@withLock it
            }

        // New slot: find the open session or create one
        val session = openSession?.takeIf { it.hasAvailableSlot() }
            ?: newSession(UUID.randomUUID().toString(), ::removeSession)
                .also { sessions[it.gameId] = it }

        session.reserveSlot(playerId)
        playerIndex[playerId] = session.gameId
        openSession = if (session.hasAvailableSlot()) session else null
        session
    }

    override fun get(gameId: String): GameSession<S, A, E>? = sessions[gameId]

    private suspend fun removeSession(gameId: String) = mutex.withLock {
        sessions.remove(gameId)
        if (openSession?.gameId == gameId) openSession = null
        playerIndex.entries.removeIf { it.value == gameId }
    }
}
