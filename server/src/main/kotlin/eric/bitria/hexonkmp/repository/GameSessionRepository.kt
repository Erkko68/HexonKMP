package eric.bitria.hexonkmp.repository

import eric.bitria.hexonkmp.session.GameSession

interface GameSessionRepository {
    suspend fun findOrJoin(playerId: String): GameSession
    fun get(gameId: String): GameSession?
}
