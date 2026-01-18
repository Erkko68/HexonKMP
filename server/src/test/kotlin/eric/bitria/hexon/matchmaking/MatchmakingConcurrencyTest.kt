package eric.bitria.hexon.matchmaking

import eric.bitria.hexon.dtos.matchmaking.JoinGameResult
import eric.bitria.hexon.services.game.GameSessionRepository
import eric.bitria.hexon.services.game.InMemoryGameSessionRepository
import eric.bitria.hexon.services.matchmaking.MatchmakingService
import eric.bitria.hexon.services.matchmaking.MatchmakingServiceImpl
import eric.bitria.hexon.ws.data.GameMode
import io.ktor.websocket.DefaultWebSocketSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchmakingConcurrencyTest {

    @Test
    fun testMatchmakingConcurrency() = runBlocking {
        val repository: GameSessionRepository = InMemoryGameSessionRepository()
        val matchmakingService: MatchmakingService = MatchmakingServiceImpl(repository)

        val playerCount = 100
        val maxPlayersPerSession = 4
        val mode = GameMode.CLASSIC

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
        val sessionsByPlayer = results.groupBy { it.sessionId }
        assertEquals(25, sessionsByPlayer.size, "Should have exactly 25 sessions for 100 players")
    }

    @Test
    fun testSessionReusabilityAfterDisconnect() = runBlocking {
        val repository: GameSessionRepository = InMemoryGameSessionRepository()
        val matchmakingService: MatchmakingService = MatchmakingServiceImpl(repository)
        val mode = GameMode.CLASSIC
        val maxPlayers = 2

        // 1. Fill a session partially
        val resp1 = matchmakingService.findGameForPlayer("user1", mode, maxPlayers)
        val sessionId = resp1.sessionId!!

        // 2. User1 disconnects (before game starts)
        val session1 = repository.getSession(sessionId)!!
        session1.removePlayer("user1")

        // 3. Next user joins - should be put into the first session again because it was returned to queue
        val resp2 = matchmakingService.findGameForPlayer("user2", mode, maxPlayers)
        assertEquals(sessionId, resp2.sessionId, "User2 should have filled the empty slot in session1")
    }

    @Test
    fun testGameStartsAutomaticallyWhenFull() = runBlocking {
        val repository: GameSessionRepository = InMemoryGameSessionRepository()
        val matchmakingService: MatchmakingService = MatchmakingServiceImpl(repository)
        val mode = GameMode.CLASSIC
        val maxPlayers = 2

        // 1. First player joins and connects
        val resp1 = matchmakingService.findGameForPlayer("user1", mode, maxPlayers)
        val session = repository.getSession(resp1.sessionId!!)!!
        val fakeWs1 = createFakeWebSocketSession()
        session.connectPlayer("user1", "user1",fakeWs1)
        
        assertTrue(!session.isGameStarted, "Game should not be started with 1 player")

        // 2. Second player joins and connects
        val resp2 = matchmakingService.findGameForPlayer("user2", mode, maxPlayers)
        val fakeWs2 = createFakeWebSocketSession()
        session.connectPlayer("user2","user2", fakeWs2)

        // 3. Verify game started
        assertTrue(session.isGameStarted, "Game should have started automatically when full")
        
        // 4. Verify it's no longer in the available pool
        val available = repository.findAvailableSession(mode)
        assertTrue(available?.sessionId != session.sessionId, "Started session should not be available for matchmaking")
    }

    private fun createFakeWebSocketSession(): DefaultWebSocketSession {
        return Proxy.newProxyInstance(
            DefaultWebSocketSession::class.java.classLoader,
            arrayOf(DefaultWebSocketSession::class.java)
        ) { _, _, _ -> null } as DefaultWebSocketSession
    }
}
