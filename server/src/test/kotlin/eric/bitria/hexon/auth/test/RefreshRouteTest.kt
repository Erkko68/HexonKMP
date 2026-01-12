package eric.bitria.hexon.auth.test

import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockLoginService
import eric.bitria.hexon.auth.mock.MockRefreshService
import eric.bitria.hexon.auth.mock.MockRegisterService
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.services.auth.repository.User
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.routes.authRoutes
import eric.bitria.hexon.utils.TokenHasher
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RefreshRouteTest {

    private val authRepository = MockAuthRepository()
    private val tokenService = MockTokenService()
    private val refreshService = MockRefreshService(authRepository, tokenService)

    private fun testAuthApplication(block: suspend (HttpClient) -> Unit) = testApplication {
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
        routing {
            authRoutes(
                registerService = MockRegisterService(authRepository),
                loginService = MockLoginService(authRepository),
                refreshService = refreshService
            )
        }
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
        block(client)
    }

    private suspend fun HttpClient.postRefresh(token: String): RefreshResponse {
        val response = post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(token))
        }
        return response.body()
    }

    @Test
    fun `refresh success and rotates tokens`() = testAuthApplication { client ->
        // 1. Setup user with an active session
        val userId = "user-1"
        val initialRefreshToken = tokenService.generateRefreshToken(userId)
        authRepository.addUser(User(userId, "test@example.com", "user", "pass", true, TokenHasher.hash(initialRefreshToken)))

        // 2. Perform refresh
        val response = client.postRefresh(initialRefreshToken)

        assertEquals(RefreshResult.SUCCESS, response.result)
        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)
        assertNotEquals(initialRefreshToken, response.refreshToken)

        // 3. Verify state: DB should now have the NEW refresh token hash
        val newHash = authRepository.getRefreshTokenHash(userId)
        assertTrue(TokenHasher.verify(response.refreshToken!!, newHash!!))
    }

    @Test
    fun `refresh fails with invalid jwt`() = testAuthApplication { client ->
        val response = client.postRefresh("not-a-valid-token")
        assertEquals(RefreshResult.INVALID_TOKEN, response.result)
    }

    @Test
    fun `refresh fails if user not found`() = testAuthApplication { client ->
        // Valid JWT structure but user doesn't exist in DB
        val token = tokenService.generateRefreshToken("ghost-id")
        val response = client.postRefresh(token)

        assertEquals(RefreshResult.USER_NOT_FOUND, response.result)
    }

    @Test
    fun `refresh fails if session was cleared (logout)`() = testAuthApplication { client ->
        val userId = "user-1"
        val token = tokenService.generateRefreshToken(userId)
        // User exists but has no refresh token hash in DB (logged out)
        authRepository.addUser(User(userId, "test@example.com", "user", "pass", true, null))

        val response = client.postRefresh(token)
        assertEquals(RefreshResult.INVALID_TOKEN, response.result)
    }

    @Test
    fun `security check - refresh fails on token reuse and revokes session`() = testAuthApplication { client ->
        val userId = "user-1"
        val oldToken = tokenService.generateRefreshToken(userId)
        val currentToken = tokenService.generateRefreshToken(userId)
        
        // DB only knows about the 'currentToken'
        authRepository.addUser(User(userId, "t@e.com", "u", "p", true, TokenHasher.hash(currentToken)))

        // Attacker tries to use 'oldToken'
        val response = client.postRefresh(oldToken)

        assertEquals(RefreshResult.TOKEN_MISMATCH, response.result)
        
        // SECURITY: Verify that the current session was revoked as a precaution
        assertNull(authRepository.getRefreshTokenHash(userId), "Session should be revoked after reuse detection")
    }

    @Test
    fun `refresh fails with malformed json`() = testApplication {
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
        routing {
            authRoutes(MockRegisterService(MockAuthRepository()), MockLoginService(MockAuthRepository()), refreshService)
        }
        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("{ \"refreshToken\": }") 
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
