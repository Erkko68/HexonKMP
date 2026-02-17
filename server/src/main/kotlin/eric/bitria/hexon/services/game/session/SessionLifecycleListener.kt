package eric.bitria.hexon.services.game.session

/**
 * Callback interface for session lifecycle events.
 * Used by repositories to manage session cleanup and tracking.
 */
interface SessionLifecycleListener {

    /**
     * Called when a game is starting and a unique gameId has been generated.
     * @param sessionId The persistent lobby/session identifier
     * @param gameId The unique identifier for this game instance (for future history tracking)
     */
    fun onGameStarting(sessionId: String, gameId: String)

    /**
     * Called when a game has ended.
     * @param sessionId The persistent lobby/session identifier
     * @param gameId The unique identifier for this game instance
     */
    fun onGameEnded(sessionId: String, gameId: String)

    /**
     * Called when a session becomes empty and should be cleaned up.
     * For matchmaking sessions: called when game ends
     * For custom lobbies: called when last player leaves
     * @param sessionId The session to remove
     */
    fun onSessionEmpty(sessionId: String)
}

