package eric.bitria.hexonkmp.repository

import eric.bitria.hexonkmp.core.game.model.Redactable
import eric.bitria.hexonkmp.session.GameSession

// Matchmaking store, generic over a game's state [S], action [A], event [E] — it
// knows nothing about Catan, only how to find/create sessions and route players.
interface GameSessionRepository<S : Redactable<S>, A, E : Redactable<E>> {
    suspend fun findOrJoin(playerId: String): GameSession<S, A, E>

    // Creates a private (manual-start) lobby hosted by [hostId] and returns it with
    // its short join code.
    suspend fun createLobby(hostId: String): Pair<GameSession<S, A, E>, String>

    // Resolves a lobby join [code] and reserves [playerId]'s seat, or null if the
    // code is unknown or the lobby is full.
    suspend fun joinByCode(code: String, playerId: String): GameSession<S, A, E>?

    fun get(gameId: String): GameSession<S, A, E>?
}
