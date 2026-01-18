package eric.bitria.hexon.services.game

import eric.bitria.hexon.ws.lobby.GameMode

interface GameSessionRepository {

    /** Add a new session to repository */
    fun addSession(mode: GameMode, session: GameSession)

    /** Remove session (finished or abandoned) */
    fun removeSession(mode: GameMode, sessionId: String)

    /** Lookup a session by id */
    fun getSession(sessionId: String): GameSession?

    /** Find a session with available slots for the given mode */
    fun findAvailableSession(mode: GameMode): GameSession?
}
