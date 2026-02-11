package eric.bitria.hexon.auth.test

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.dtos.auth.*
import eric.bitria.hexon.email.mock.MockEmailVerificationRepository
import eric.bitria.hexon.email.mock.MockSmtpService
import eric.bitria.hexon.routes.authRoutes
import eric.bitria.hexon.security.UserSession
import eric.bitria.hexon.services.auth.login.LoginService
import eric.bitria.hexon.services.auth.login.LoginServiceImpl
import eric.bitria.hexon.services.auth.logout.LogoutService
import eric.bitria.hexon.services.auth.logout.LogoutServiceImpl
import eric.bitria.hexon.services.auth.refresh.RefreshService
import eric.bitria.hexon.services.auth.refresh.RefreshServiceImpl
import eric.bitria.hexon.services.auth.register.RegisterService
import eric.bitria.hexon.services.auth.register.RegisterServiceImpl
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.repository.User
import eric.bitria.hexon.services.auth.token.TokenService
import eric.bitria.hexon.services.email.repository.EmailVerificationRepository
import eric.bitria.hexon.services.email.smtp.SmtpService
import eric.bitria.hexon.services.email.verification.EmailVerificationService
import eric.bitria.hexon.services.email.verification.EmailVerificationServiceImpl
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

class RefreshRouteTest {

    private val authRepository = MockAuthRepository()
    private val emailVerificationRepository = MockEmailVerificationRepository()
    private val smtpService = MockSmtpService()
    private val tokenService = MockTokenService()

    private val emailVerificationService = EmailVerificationServiceImpl(
        emailVerificationRepository,
        smtpService,
        authRepository
    )

    private val loginService = LoginServiceImpl(authRepository, tokenService)
    private val registerService = RegisterServiceImpl(authRepository, emailVerificationService)
    private val refreshService = RefreshServiceImpl(authRepository, tokenService)
    private val logoutService = LogoutServiceImpl(authRepository, tokenService)

    private fun testAuthApplication(block: suspend (HttpClient) -> Unit) = testApplication {
        install(Koin) {
            modules(module {
                single<AuthRepository> { authRepository }
                single<EmailVerificationRepository> { emailVerificationRepository }
                single<SmtpService> { smtpService }
                single<TokenService> { tokenService }
                single<EmailVerificationService> { emailVerificationService }
                single<RegisterService> { registerService }
                single<LoginService> { loginService }
                single<RefreshService> { refreshService }
                single<LogoutService> { logoutService }
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
        val password = "Password123!"
        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        authRepository.addUser(User("user-1", email, "user", passwordHash, true))

        val loginResp: LoginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()

        val oldRefreshToken = loginResp.refreshToken!!
        val oldHash = TokenHasher.hash(oldRefreshToken)

        // 2. Perform refresh (uses cookie from login)
        val response: RefreshResponse = client.post("/auth/refresh").body()

        assertEquals(RefreshResult.SUCCESS, response.result)
        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)
        assertNotEquals(oldRefreshToken, response.refreshToken, "New refresh token should be different")

        // 3. Verify state: Old token should be gone, new token should exist
        val newHash = TokenHasher.hash(response.refreshToken!!)
        assertFalse(authRepository.hasRefreshTokenHash(oldHash), "Old refresh token should be invalidated")
        assertTrue(authRepository.hasRefreshTokenHash(newHash), "New refresh token should be stored")
    }

    @Test
    fun `refresh fails with no session cookie`() = testAuthApplication { client ->
        val response = client.post("/auth/refresh")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `refresh fails if user not found`() = testAuthApplication { client ->
        val email = "ghost@example.com"
        val password = "Password123!"
        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val userId = "ghost-id"
        authRepository.addUser(User(userId, email, "ghost", passwordHash, true))

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
        val password = "Password123!"
        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val userId = "user-logout"
        authRepository.addUser(User(userId, email, "user", passwordHash, true))

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
