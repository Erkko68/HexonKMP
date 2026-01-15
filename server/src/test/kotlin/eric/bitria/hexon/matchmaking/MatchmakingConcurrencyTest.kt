package eric.bitria.hexon.matchmaking

import eric.bitria.hexon.dtos.matchmaking.JoinGameResult
import eric.bitria.hexon.services.game.GameSessionRepository
import eric.bitria.hexon.services.game.InMemoryGameSessionRepository
import eric.bitria.hexon.services.matchmaking.MatchmakingService
import eric.bitria.hexon.services.matchmaking.MatchmakingServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MatchmakingConcurrencyTest {

    @Test
    fun testMatchmakingConcurrency() = runBlocking {
        val repository: GameSessionRepository = InMemoryGameSessionRepository()
        val matchmakingService: MatchmakingService = MatchmakingServiceImpl(repository)

        val playerCount = 100
        val maxPlayersPerSession = 4
        val mode = "classic"

        // Simulate 100 players joining simultaneously
        val jobs = (1..playerCount).map { i ->
            async(Dispatchers.Default) {
                matchmakingService.findGameForPlayer(
                    userId = "user_$i",
                    mode = mode,
                    maxPlayers = maxPlayersPerSession
                )
            }
        }

        val results = jobs.awaitAll()

        // Verify all joined successfully
        results.forEach { result ->
            assertEquals(JoinGameResult.SUCCESS, result.status, "Player failed to join: ${result.message}")
            assertTrue(result.sessionId != null, "SessionId should not be null")
        }

        // Verify number of sessions created
        // With 100 players and 4 players per session, we expect exactly 25 sessions
        val sessionsByPlayer = results.groupBy { it.sessionId }
        assertEquals(25, sessionsByPlayer.size, "Should have exactly 25 sessions for 100 players")

        // Verify each session has exactly 4 reservations
        sessionsByPlayer.forEach { (sessionId, reservations) ->
            assertEquals(4, reservations.size, "Session $sessionId should have 4 reservations")
            
            val session = repository.getSession(sessionId!!)
            assertEquals(4, session?.reservedPlayers()?.size, "Session $sessionId should have 4 players in repository")
        }
    }

    @Test
    fun testSessionReusabilityAfterDisconnect() = runBlocking {
        val repository: GameSessionRepository = InMemoryGameSessionRepository()
        val matchmakingService: MatchmakingService = MatchmakingServiceImpl(repository)
        val mode = "classic"
        val maxPlayers = 2

        // 1. Fill a session
        matchmakingService.findGameForPlayer("user1", mode, maxPlayers)
        val resp2 = matchmakingService.findGameForPlayer("user2", mode, maxPlayers)
        val sessionId = resp2.sessionId!!

        // 2. Try to join another player - should create a NEW session
        val resp3 = matchmakingService.findGameForPlayer("user3", mode, maxPlayers)
        assertNotEquals(sessionId, resp3.sessionId, "Should have created a new session")

        // 3. User1 disconnects from the first session (before it starts)
        val session1 = repository.getSession(sessionId)!!
        session1.removePlayer("user1")

        // 4. Next user joins - should be put into the first session again because it has a free slot
        val resp4 = matchmakingService.findGameForPlayer("user4", mode, maxPlayers)
        assertEquals(sessionId, resp4.sessionId, "User4 should have filled the empty slot in session1")
    }

    @Test
    fun testReservationTimeout() = runBlocking {
        val repository: GameSessionRepository = InMemoryGameSessionRepository()
        val matchmakingService: MatchmakingService = MatchmakingServiceImpl(repository)
        val mode = "classic"
        val maxPlayers = 1

        // 1. Reserve a slot
        val resp1 = matchmakingService.findGameForPlayer("user1", mode, maxPlayers)
        val sessionId = resp1.sessionId!!

        // 2. Wait for timeout (GameSessionImpl default or we can force it)
        // Note: reserveSlot calls cleanupExpiredSlots internally
        // Let's manually trigger cleanup with a very small timeout for testing
        val session = repository.getSession(sessionId)!!
        
        // Wait a bit to ensure the timestamp is older than our test timeout
        delay(10) 
        
        // This will find the session, see it's "full" (1/1), but cleanup will remove user1
        // because we passed 1ms timeout.
        session.cleanupExpiredSlots(timeoutMs = 1)
        
        // 3. Next user joins - should be able to take the same session
        val resp2 = matchmakingService.findGameForPlayer("user2", mode, maxPlayers)
        assertEquals(sessionId, resp2.sessionId, "User2 should take the session after User1's reservation expired")
        assertEquals(0, session.reservedPlayers().filter { it == "user1" }.size)
        assertEquals(1, session.reservedPlayers().filter { it == "user2" }.size)
    }

    @Test
    fun testHeavyLoadMixedJoinLeave() = runBlocking {
        val repository: GameSessionRepository = InMemoryGameSessionRepository()
        val matchmakingService: MatchmakingService = MatchmakingServiceImpl(repository)
        val mode = "classic"
        val maxPlayers = 4
        val totalPlayers = 200

        // Randomly join and leave
        val jobs = (1..totalPlayers).map { i ->
            async(Dispatchers.Default) {
                val joinResp = matchmakingService.findGameForPlayer("user_$i", mode, maxPlayers)
                if (i % 3 == 0) { // Every 3rd player leaves immediately
                    delay(5)
                    repository.getSession(joinResp.sessionId!!)?.removePlayer("user_$i")
                }
                joinResp
            }
        }

        val results = jobs.awaitAll()
        
        // Just verify no exceptions occurred and we have valid sessions
        results.forEach { assertEquals(JoinGameResult.SUCCESS, it.status) }
        
        val activeSessions = results.mapNotNull { it.sessionId }.distinct()
        activeSessions.forEach { sessionId ->
            val session = repository.getSession(sessionId)
            assertTrue(session!!.connectedPlayers().size + session.reservedPlayers().size <= maxPlayers)
        }
    }
    
    @Test
    fun testIdempotentJoin() = runBlocking {
        val repository: GameSessionRepository = InMemoryGameSessionRepository()
        val matchmakingService: MatchmakingService = MatchmakingServiceImpl(repository)
        val mode = "classic"
        val maxPlayers = 4

        // User joins twice
        val resp1 = matchmakingService.findGameForPlayer("user1", mode, maxPlayers)
        val resp2 = matchmakingService.findGameForPlayer("user1", mode, maxPlayers)

        assertEquals(resp1.sessionId, resp2.sessionId, "Same user joining twice should get same sessionId")
        
        val session = repository.getSession(resp1.sessionId!!)!!
        assertEquals(1, session.reservedPlayers().size, "User should only occupy one slot")
    }
}
