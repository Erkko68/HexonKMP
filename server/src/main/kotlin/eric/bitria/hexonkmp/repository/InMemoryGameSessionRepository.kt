package eric.bitria.hexonkmp.repository

import eric.bitria.hexonkmp.core.game.model.Redactable
import eric.bitria.hexonkmp.session.GameSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

// Game-agnostic matchmaking: it creates sessions through an injected factory
// (which knows the concrete game — its engine, codec, and match config) and never
// names a game type itself.
class InMemoryGameSessionRepository<S : Redactable<S>, A, E : Redactable<E>>(
    // (gameId, manualStart, onEmpty) -> a fresh session for this game. manualStart
    // selects the lobby policy (auto matchmaking vs host-started private lobby);
    // onEmpty lets the session tell the repository to clean up when it empties.
    private val newSession: (
        gameId: String,
        manualStart: Boolean,
        onEmpty: suspend (gameId: String) -> Unit,
    ) -> GameSession<S, A, E>,
) : GameSessionRepository<S, A, E> {
    private val sessions = ConcurrentHashMap<String, GameSession<S, A, E>>()
    private val playerIndex = ConcurrentHashMap<String, String>() // playerId → gameId
    private val codeIndex = ConcurrentHashMap<String, String>()    // lobby code → gameId
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
            ?: newSession(UUID.randomUUID().toString(), false, ::removeSession)
                .also { sessions[it.gameId] = it }

        session.reserveSlot(playerId)
        playerIndex[playerId] = session.gameId
        openSession = if (session.hasAvailableSlot()) session else null
        session
    }

    override suspend fun createLobby(hostId: String): Pair<GameSession<S, A, E>, String> = mutex.withLock {
        // A private lobby is a manual-start session, never offered to matchmaking
        // (it's not assigned to openSession). It's reachable only via its join code.
        val session = newSession(UUID.randomUUID().toString(), true, ::removeSession)
            .also { sessions[it.gameId] = it }
        val code = generateUnusedCode()
        codeIndex[code] = session.gameId
        session.reserveSlot(hostId)
        playerIndex[hostId] = session.gameId
        session to code
    }

    override suspend fun joinByCode(code: String, playerId: String): GameSession<S, A, E>? = mutex.withLock {
        val session = codeIndex[code]?.let { sessions[it] } ?: return@withLock null
        // Reconnect: already mapped to this session — re-reserve the freed slot.
        if (playerIndex[playerId] == session.gameId) {
            session.reserveSlot(playerId)
            return@withLock session
        }
        if (!session.hasAvailableSlot()) return@withLock null
        session.reserveSlot(playerId)
        playerIndex[playerId] = session.gameId
        session
    }

    override fun get(gameId: String): GameSession<S, A, E>? = sessions[gameId]

    // A random unused 6-digit code. At our scale (few concurrent lobbies) collisions
    // are rare, so a retry loop is plenty; codes are freed when the session empties.
    private fun generateUnusedCode(): String {
        while (true) {
            val code = Random.nextInt(0, 1_000_000).toString().padStart(6, '0')
            if (!codeIndex.containsKey(code)) return code
        }
    }

    private suspend fun removeSession(gameId: String) = mutex.withLock {
        sessions.remove(gameId)
        if (openSession?.gameId == gameId) openSession = null
        playerIndex.entries.removeIf { it.value == gameId }
        codeIndex.entries.removeIf { it.value == gameId }
    }
}
