package eric.bitria.hexon.auth.test

import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockLoginService
import eric.bitria.hexon.auth.mock.MockLogoutService
import eric.bitria.hexon.auth.mock.MockRefreshService
import eric.bitria.hexon.auth.mock.MockRegisterService
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse
import eric.bitria.hexon.dtos.auth.LogoutRequest
import eric.bitria.hexon.dtos.auth.LogoutResponse
import eric.bitria.hexon.dtos.auth.LogoutResult
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
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

class LogoutRouteTest {

    private val authRepository = MockAuthRepository()
    private val tokenService = MockTokenService()
    private val logoutService = MockLogoutService(authRepository, tokenService)

    private fun testAuthApplication(block: suspend (HttpClient) -> Unit) = testApplication {
        install(Koin) {
            modules(module {
                single<AuthRepository> { authRepository }
                single<TokenService> { tokenService }
                single<RegisterService> { MockRegisterService(authRepository) }
                single<LoginService> { MockLoginService(authRepository, tokenService) }
                single<RefreshService> { MockRefreshService(authRepository, tokenService) }
                single<LogoutService> { logoutService }
            })
        }
        install(ContentNegotiation) {
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
    fun `logout success clears cookie and revokes session`() = testAuthApplication { client ->
        val email = "logout@example.com"
        val password = "pass"
        authRepository.addUser(User("user-1", email, "user", password, true))

        // 1. Login
        val loginResp: LoginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()
        
        val refreshToken = loginResp.refreshToken!!
        val hash = TokenHasher.hash(refreshToken)

        // 2. Logout
        val logoutResp: LogoutResponse = client.post("/auth/logout").body()

        assertEquals(LogoutResult.SUCCESS, logoutResp.result)
        
        // 3. Verify session revoked in DB
        assertFalse(authRepository.hasRefreshTokenHash(hash))
        
        // 4. Verify cookie is cleared (attempting refresh should fail with 401)
        val refreshResp = client.post("/auth/refresh")
        assertEquals(HttpStatusCode.BadRequest, refreshResp.status)
    }

    @Test
    fun `logout all devices revokes multiple sessions`() = testAuthApplication { client ->
        val email = "multi@example.com"
        val password = "pass"
        val userId = "user-multi"
        authRepository.addUser(User(userId, email, "user", password, true))

        // Simulate 3 sessions
        val token1 = tokenService.generateRefreshToken(userId)
        val token2 = tokenService.generateRefreshToken(userId)
        val token3 = tokenService.generateRefreshToken(userId)
        
        val expiresAt = java.time.LocalDateTime.now().plusDays(7)
        authRepository.addRefreshToken(userId, TokenHasher.hash(token1), expiresAt)
        authRepository.addRefreshToken(userId, TokenHasher.hash(token2), expiresAt)
        authRepository.addRefreshToken(userId, TokenHasher.hash(token3), expiresAt)

        // Login to get a valid session cookie for the logout call
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }

        // Logout all devices
        val logoutResp: LogoutResponse = client.post("/auth/logout") {
            contentType(ContentType.Application.Json)
            setBody(LogoutRequest(logoutAllDevices = true))
        }.body()

        assertEquals(LogoutResult.SUCCESS, logoutResp.result)

        // Verify ALL sessions for this user are gone
        assertFalse(authRepository.hasRefreshTokenHash(TokenHasher.hash(token1)))
        assertFalse(authRepository.hasRefreshTokenHash(TokenHasher.hash(token2)))
        assertFalse(authRepository.hasRefreshTokenHash(TokenHasher.hash(token3)))
    }
}
