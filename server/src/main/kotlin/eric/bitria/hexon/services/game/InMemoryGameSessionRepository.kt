package eric.bitria.hexon.services.game

import eric.bitria.hexon.services.game.session.BaseGameSession
import eric.bitria.hexon.services.game.session.SessionLifecycleListener
import eric.bitria.hexon.ws.lobby.GameMode
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class InMemoryGameSessionRepository : GameSessionRepository, SessionLifecycleListener {

    private val logger = LoggerFactory.getLogger(InMemoryGameSessionRepository::class.java)

    // mode -> Queue of sessions with available slots
    private val availableQueuesByMode = ConcurrentHashMap<GameMode, LinkedBlockingQueue<BaseGameSession>>()

    // global lookup for sessionId -> BaseGameSession
    private val allSessions = ConcurrentHashMap<String, BaseGameSession>()

    // Track which mode each session belongs to for cleanup
    private val sessionModes = ConcurrentHashMap<String, GameMode>()

    override fun addSession(mode: GameMode, session: BaseGameSession) {
        allSessions[session.sessionId] = session
        sessionModes[session.sessionId] = mode

        // Register this repository as lifecycle listener
        session.setLifecycleListener(this)

        // Only add to available queue if it has slots
        if (session.hasAvailableSlots()) {
            val queue = availableQueuesByMode.computeIfAbsent(mode) { LinkedBlockingQueue() }
            if (!queue.contains(session)) {
                queue.add(session)
            }
        }

        logger.info("Added session ${session.sessionId} for mode $mode")
    }

    override fun removeSession(mode: GameMode, sessionId: String) {
        val session = allSessions.remove(sessionId)
        sessionModes.remove(sessionId)

        if (session != null) {
            availableQueuesByMode[mode]?.remove(session)
            logger.info("Removed session $sessionId from mode $mode")
        }
    }

    override fun getSession(sessionId: String): BaseGameSession? {
        return allSessions[sessionId]
    }

    override fun findAvailableSession(mode: GameMode): BaseGameSession? {
        val queue = availableQueuesByMode[mode] ?: return null
        
        while (true) {
            val session = queue.peek() ?: return null

            // Check if it has space
            if (session.hasAvailableSlots()) {
                return session
            } else {
                // Session is full, remove it from the "Available" queue
                queue.remove(session)
            }
        }
    }

    // ==================== SessionLifecycleListener Implementation ====================

    override fun onGameStarting(sessionId: String, gameId: String) {
        logger.info("Game $gameId starting in session $sessionId")
        // Future: Could track active games here for monitoring
    }

    override fun onGameEnded(sessionId: String, gameId: String) {
        logger.info("Game $gameId ended in session $sessionId")
        // Future: Could record game history here
    }

    override fun onSessionEmpty(sessionId: String) {
        logger.info("Session $sessionId is empty, cleaning up")
        val mode = sessionModes[sessionId]
        if (mode != null) {
            removeSession(mode, sessionId)
        } else {
            // Fallback: try to remove from all modes
            allSessions.remove(sessionId)
            sessionModes.remove(sessionId)
            availableQueuesByMode.values.forEach { queue ->
                queue.removeIf { it.sessionId == sessionId }
            }
        }
    }
}
