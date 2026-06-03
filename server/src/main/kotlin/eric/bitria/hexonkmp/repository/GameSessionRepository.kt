package eric.bitria.hexonkmp.repository

import eric.bitria.hexonkmp.core.game.model.Redactable
import eric.bitria.hexonkmp.session.GameSession

// Matchmaking store, generic over a game's state [S], action [A], event [E] — it
// knows nothing about Catan, only how to find/create sessions and route players.
interface GameSessionRepository<S : Redactable<S>, A, E : Redactable<E>> {
    suspend fun findOrJoin(playerId: String): GameSession<S, A, E>
    fun get(gameId: String): GameSession<S, A, E>?
}
