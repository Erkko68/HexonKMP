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
        val activePlayersCount = AtomicInteger(0)
        val successfulJoins = AtomicInteger(0)
        val timeoutsCount = AtomicInteger(0)
        val leavesCount = AtomicInteger(0)

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
                        // 10%: "Ghost" player - reserves but never connects
                        delay(50) 
                        session.cleanupExpiredSlots(timeoutMs = 10) 
                        timeoutsCount.incrementAndGet()
                    }
                    behavior < 30 -> {
                        // 20%: Connects then leaves almost immediately
                        activePlayersCount.incrementAndGet()
                        val ws = null as DefaultWebSocketSession? 
                        if (session.connectPlayer(userId, ws ?: return@async)) {
                            delay(Random.nextLong(1, 10))
                            session.removePlayer(userId)
                            leavesCount.incrementAndGet()
                        }
                        activePlayersCount.decrementAndGet()
                    }
                    behavior < 80 -> {
                        // 50%: Connects and stays for a while (normal player)
                        activePlayersCount.incrementAndGet()
                        val ws = null as DefaultWebSocketSession?
                        if (session.connectPlayer(userId, ws ?: return@async)) {
                            delay(Random.nextLong(50, 200))
                            session.removePlayer(userId)
                            leavesCount.incrementAndGet()
                        }
                        activePlayersCount.decrementAndGet()
                    }
                    else -> {
                        // 20%: Joins but disconnects/cancels BEFORE connecting
                        delay(Random.nextLong(1, 10))
                        session.removePlayer(userId)
                        leavesCount.incrementAndGet()
                    }
                }
            }
        }

        // Wait for all player actions to complete
        withTimeout(20000) { 
            playerJobs.awaitAll()
        }

        // Final grace period and exhaustive cleanup to ensure all "Ghosts" are purged
        delay(100)
        val allSessions = getAllSessionsFromRepo(repository, mode)
        allSessions.forEach { it.cleanupExpiredSlots(timeoutMs = 0) }

        // --- Final Verification ---
        
        println("Stress Test Stats:")
        println("Successful Joins: ${successfulJoins.get()}")
        println("Timeouts: ${timeoutsCount.get()}")
        println("Leaves/Disconnects: ${leavesCount.get()}")
        println("Sessions Created: ${allSessions.size}")
        
        allSessions.forEach { session ->
            val total = session.connectedPlayers().size + session.reservedPlayers().size
            assertTrue(total <= maxPlayers, "Session ${session.sessionId} exceeded max players: $total")
            assertTrue(session.connectedPlayers().isEmpty(), "Session ${session.sessionId} should be empty of connected players")
            assertTrue(session.reservedPlayers().isEmpty(), "Session ${session.sessionId} should be empty of reserved players")
        }
    }

    private fun getAllSessionsFromRepo(repo: GameSessionRepository, mode: String): List<GameSession> {
        val field = repo.javaClass.getDeclaredField("allSessions")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val allSessionsMap = field.get(repo) as Map<String, GameSession>
        return allSessionsMap.values.filter { it.mode == mode }
    }
}
