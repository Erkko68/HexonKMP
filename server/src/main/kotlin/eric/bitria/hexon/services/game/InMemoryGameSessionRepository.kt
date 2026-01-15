package eric.bitria.hexon.services.game

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryGameSessionRepository : GameSessionRepository {

    // mode -> List of sessions with available slots
    private val availableSessionsByMode = ConcurrentHashMap<String, CopyOnWriteArrayList<GameSession>>()
    
    // global lookup for sessionId -> GameSession
    private val allSessions = ConcurrentHashMap<String, GameSession>()

    override fun addSession(mode: String, session: GameSession) {
        allSessions[session.sessionId] = session
        availableSessionsByMode.computeIfAbsent(mode) { CopyOnWriteArrayList() }.add(session)
    }

    override fun removeSession(mode: String, sessionId: String) {
        val session = allSessions.remove(sessionId)
        if (session != null) {
            availableSessionsByMode[mode]?.remove(session)
        }
    }

    override fun getSession(sessionId: String): GameSession? {
        return allSessions[sessionId]
    }

    override fun findAvailableSession(mode: String): GameSession? {
        val sessions = availableSessionsByMode[mode] ?: return null
        
        // Find the best session to fill
        val iterator = sessions.iterator()
        while (iterator.hasNext()) {
            val session = iterator.next()
            val currentCount = session.connectedPlayers().size + session.reservedPlayers().size
            
            if (currentCount < session.maxPlayers) {
                return session
            } else {
                // Session is full, cleanup the available list
                sessions.remove(session)
            }
        }
        return null
    }

    override fun returnSessionToQueue(mode: String, session: GameSession) {
        val sessions = availableSessionsByMode.computeIfAbsent(mode) { CopyOnWriteArrayList() }
        // Ensure it's at the BEGINNING of the list so it gets picked up immediately for reuse
        if (!sessions.contains(session)) {
            sessions.add(0, session)
        }
    }
}
