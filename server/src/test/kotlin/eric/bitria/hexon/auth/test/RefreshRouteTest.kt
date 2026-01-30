package eric.bitria.hexon.auth.test

import eric.bitria.hexon.auth.mock.*
import eric.bitria.hexon.dtos.auth.*
import eric.bitria.hexon.routes.authRoutes
import eric.bitria.hexon.security.UserSession
import eric.bitria.hexon.services.auth.login.LoginService
import eric.bitria.hexon.services.auth.logout.LogoutService
import eric.bitria.hexon.services.auth.refresh.RefreshService
import eric.bitria.hexon.services.auth.register.RegisterService
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.repository.User
import eric.bitria.hexon.services.auth.token.TokenService
import eric.bitria.hexon.utils.TokenHasher
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

class RefreshRouteTest {

    private val authRepository = MockAuthRepository()
    private val tokenService = MockTokenService()
    private val refreshService = MockRefreshService(authRepository, tokenService)

    private fun testAuthApplication(block: suspend (HttpClient) -> Unit) = testApplication {
        install(Koin) {
            modules(module {
                single<AuthRepository> { authRepository }
                single<TokenService> { tokenService }
                single<RegisterService> { MockRegisterService(authRepository) }
                single<LoginService> { MockLoginService(authRepository, tokenService) }
                single<RefreshService> { refreshService }
                single<LogoutService> { 
                    object : LogoutService {
                        override suspend fun logout(refreshToken: String, request: LogoutRequest): LogoutResponse {
                            return LogoutResponse(LogoutResult.SUCCESS, "Logged out")
                        }
                    }
                }
            })
        }
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
        install(Sessions) {
            cookie<UserSession>("USER_SESSION") {
                cookie.path = "/"
            }
        }
        routing {
            authRoutes()
        }
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
            install(HttpCookies)
        }
        block(client)
    }

    @Test
    fun `refresh success and rotates tokens`() = testAuthApplication { client ->
        // 1. Setup user with an active session by logging in
        val email = "test@example.com"
        val password = "pass"
        authRepository.addUser(User("user-1", email, "user", password, true))

        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }

        // 2. Perform refresh (uses cookie from login)
        val response: RefreshResponse = client.post("/auth/refresh").body()

        assertEquals(RefreshResult.SUCCESS, response.result)
        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)

        // 3. Verify state: DB should now have the NEW refresh token hash
        val newHash = TokenHasher.hash(response.refreshToken!!)
        assertTrue(authRepository.hasRefreshTokenHash(newHash))
    }

    @Test
    fun `refresh fails with no session cookie`() = testAuthApplication { client ->
        val response = client.post("/auth/refresh")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `refresh fails if user not found`() = testAuthApplication { client ->
        val email = "ghost@example.com"
        val password = "pass"
        val userId = "ghost-id"
        authRepository.addUser(User(userId, email, "ghost", password, true))

        // 1. Login to get session
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }

        // 2. Delete user
        authRepository.deleteUser(userId)

        // 3. Try to refresh
        val response: RefreshResponse = client.post("/auth/refresh").body()
        assertEquals(RefreshResult.USER_NOT_FOUND, response.result)
    }

    @Test
    fun `refresh fails if session was cleared (logout)`() = testAuthApplication { client ->
        val email = "logout@example.com"
        val password = "pass"
        val userId = "user-logout"
        authRepository.addUser(User(userId, email, "user", password, true))

        // 1. Login
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }

        // 2. Revoke session manually
        authRepository.revokeAllRefreshTokens(userId)

        // 3. Refresh should fail
        val response: RefreshResponse = client.post("/auth/refresh").body()
        assertEquals(RefreshResult.INVALID_TOKEN, response.result)
    }
}
