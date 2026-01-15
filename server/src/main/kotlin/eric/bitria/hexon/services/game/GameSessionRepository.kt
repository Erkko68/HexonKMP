package eric.bitria.hexon.services.game

interface GameSessionRepository {

    /** Add a new session to repository */
    fun addSession(mode: String, session: GameSession)

    /** Remove session (finished or abandoned) */
    fun removeSession(mode: String, sessionId: String)

    /** Lookup a session by id */
    fun getSession(sessionId: String): GameSession?

    /** Find a session with available slots for the given mode */
    fun findAvailableSession(mode: String): GameSession?

    /** Put a session back into the available queue if it has space */
    fun returnSessionToQueue(mode: String, session: GameSession)
}
