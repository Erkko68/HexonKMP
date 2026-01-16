package eric.bitria.hexon.services.game

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class InMemoryGameSessionRepository : GameSessionRepository {

    // mode -> Queue of sessions with available slots
    private val availableQueuesByMode = ConcurrentHashMap<String, LinkedBlockingQueue<GameSession>>()
    
    // global lookup for sessionId -> GameSession
    private val allSessions = ConcurrentHashMap<String, GameSession>()

    override fun addSession(mode: String, session: GameSession) {
        allSessions[session.sessionId] = session
        // Only add to available queue if it's not already there and has slots/not started
        if (!session.isGameStarted && session.hasAvailableSlots()) {
            val queue = availableQueuesByMode.computeIfAbsent(mode) { LinkedBlockingQueue() }
            if (!queue.contains(session)) {
                queue.add(session)
            }
        }
    }

    override fun removeSession(mode: String, sessionId: String) {
        val session = allSessions.remove(sessionId)
        if (session != null) {
            availableQueuesByMode[mode]?.remove(session)
        }
    }

    override fun getSession(sessionId: String): GameSession? {
        return allSessions[sessionId]
    }

    override fun findAvailableSession(mode: String): GameSession? {
        val queue = availableQueuesByMode[mode] ?: return null
        
        while (true) {
            val session = queue.peek() ?: return null
            
            // CLEANUP 1: If the game already started, it shouldn't be in the matching queue
            if (session.isGameStarted) {
                queue.remove(session)
                continue
            }

            // CLEANUP 2: Check if it has space
            if (session.hasAvailableSlots()) {
                return session
            } else {
                // Session is full (but not started).
                // Remove it from the "Available" queue so we don't waste time checking it again.
                queue.remove(session)
            }
        }
    }
}
