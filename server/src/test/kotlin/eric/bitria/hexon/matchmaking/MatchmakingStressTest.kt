package eric.bitria.hexon.matchmaking

import eric.bitria.hexon.dtos.matchmaking.JoinGameResult
import eric.bitria.hexon.services.game.GameSession
import eric.bitria.hexon.services.game.GameSessionRepository
import eric.bitria.hexon.services.game.InMemoryGameSessionRepository
import eric.bitria.hexon.services.matchmaking.MatchmakingService
import eric.bitria.hexon.services.matchmaking.MatchmakingServiceImpl
import io.ktor.websocket.DefaultWebSocketSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.test.assertTrue

class MatchmakingStressTest {

    @Test
    fun testIntenseRandomMatchmaking() = runBlocking {
        val repository: GameSessionRepository = InMemoryGameSessionRepository()
        val matchmakingService: MatchmakingService = MatchmakingServiceImpl(repository)
        val mode = "classic"
        val maxPlayers = 4
        
        val totalPlayersToSimulate = 500
        val successfulJoins = AtomicInteger(0)
        val leavesCount = AtomicInteger(0)

        val fakeWs = createFakeWebSocketSession()

        val playerJobs = (1..totalPlayersToSimulate).map { i ->
            async(Dispatchers.Default) {
                val userId = "user_$i"
                
                // Random delay before joining to spread the load
                delay(Random.nextLong(0, 500))

                val joinResp = matchmakingService.findGameForPlayer(userId, mode, maxPlayers)
                if (joinResp.status != JoinGameResult.SUCCESS) return@async
                
                successfulJoins.incrementAndGet()
                val sessionId = joinResp.sessionId!!
                val session = repository.getSession(sessionId)!!

                // Decide what this player does
                val behavior = Random.nextInt(100)
                when {
                    behavior < 10 -> {
                        // 10%: Join but cancel before connecting
                        delay(Random.nextLong(5, 15))
                        session.removePlayer(userId)
                        leavesCount.incrementAndGet()
                    }
                    else -> {
                        // 90%: Connect and play
                        if (session.connectPlayer(userId, userId,fakeWs)) {
                            // If game started, they stay longer
                            if (session.isGameStarted) {
                                delay(Random.nextLong(100, 300))
                            } else {
                                // If not started, they might leave early
                                if (Random.nextBoolean()) {
                                    delay(Random.nextLong(5, 50))
                                    session.removePlayer(userId)
                                    leavesCount.incrementAndGet()
                                } else {
                                    // Wait for game start or someone else to fill
                                    delay(Random.nextLong(100, 300))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Wait for all player actions to complete
        withTimeout(20000) { 
            playerJobs.awaitAll()
        }

        // --- Final Verification ---
        val allSessions = getAllSessionsFromRepo(repository, mode)
        
        println("Stress Test Stats:")
        println("Successful Joins: ${successfulJoins.get()}")
        println("Leaves: ${leavesCount.get()}")
        println("Sessions Created: ${allSessions.size}")
        
        allSessions.forEach { session ->
            // Verify capacity constraints
            // We use reflection to access private fields for verification since they aren't in the interface
            val connectedField = session.javaClass.getDeclaredField("connectedPlayers")
            connectedField.isAccessible = true
            val connectedCount = (connectedField.get(session) as Map<*, *>).size

            val reservedField = session.javaClass.getDeclaredField("reservedSlots")
            reservedField.isAccessible = true
            val reservedCount = (reservedField.get(session) as Map<*, *>).size

            val maxPlayersField = session.javaClass.getDeclaredField("maxPlayers")
            maxPlayersField.isAccessible = true
            val max = maxPlayersField.get(session) as Int

            assertTrue(connectedCount + reservedCount <= max, "Session ${session.sessionId} exceeded capacity")
        }
    }

    private fun createFakeWebSocketSession(): DefaultWebSocketSession {
        return Proxy.newProxyInstance(
            DefaultWebSocketSession::class.java.classLoader,
            arrayOf(DefaultWebSocketSession::class.java)
        ) { _, _, _ -> null } as DefaultWebSocketSession
    }

    private fun getAllSessionsFromRepo(repo: GameSessionRepository, mode: String): List<GameSession> {
        val field = repo.javaClass.getDeclaredField("allSessions")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val allSessionsMap = field.get(repo) as Map<String, GameSession>
        
        // Since 'mode' is private in GameSessionImpl and not in GameSession interface,
        // we use reflection to filter sessions by mode.
        return allSessionsMap.values.filter { session ->
            try {
                val modeField = session.javaClass.getDeclaredField("mode")
                modeField.isAccessible = true
                modeField.get(session) == mode
            } catch (e: Exception) {
                false
            }
        }
    }
}
