package eric.bitria.hexon.auth.test

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse
import eric.bitria.hexon.dtos.auth.LoginResult
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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

class LoginRouteTest {

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
            install(ClientContentNegotiation) {
                json()
            }
        }
        block(client)
    }

    private suspend fun HttpClient.postLogin(request: Any): LoginResponse {
        val response = post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body()
    }

    @Test
    fun `login success with valid credentials`() = testAuthApplication { client ->
        val email = "test@example.com"
        val password = "Password123!"
        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        authRepository.addUser(User("user-1", email, "testuser", passwordHash, true))

        val request = LoginRequest(email, password)
        val response = client.postLogin(request)

        assertEquals(LoginResult.SUCCESS, response.result)
        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)
    }

    @Test
    fun `login fails with wrong password`() = testAuthApplication { client ->
        val email = "test@example.com"
        val correctPassword = "CorrectPassword123!"
        val passwordHash = BCrypt.withDefaults().hashToString(12, correctPassword.toCharArray())
        authRepository.addUser(User("user-1", email, "testuser", passwordHash, true))

        val request = LoginRequest(email, "WrongPassword123!")
        val response = client.postLogin(request)

        assertEquals(LoginResult.INVALID_CREDENTIALS, response.result)
        assertNull(response.accessToken)
        assertNull(response.refreshToken)
    }

    @Test
    fun `login fails with unverified account`() = testAuthApplication { client ->
        val email = "unverified@example.com"
        val password = "Password123!"
        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        authRepository.addUser(User("user-2", email, "unverified", passwordHash, false))

        val request = LoginRequest(email, password)
        val response = client.postLogin(request)

        assertEquals(LoginResult.NOT_VERIFIED, response.result)
        assertNull(response.accessToken)
        assertNull(response.refreshToken)
    }

    @Test
    fun `login fails with non-existent user`() = testAuthApplication { client ->
        val request = LoginRequest("ghost@example.com", "Password123!")
        val response = client.postLogin(request)

        assertEquals(LoginResult.INVALID_CREDENTIALS, response.result)
        assertNull(response.accessToken)
    }

    @Test
    fun `login fails with invalid email format`() = testAuthApplication { client ->
        val request = LoginRequest("invalid-email", "Password123!")
        val response = client.postLogin(request)

        assertEquals(LoginResult.INVALID_CREDENTIALS, response.result)
        assertNull(response.accessToken)
    }

    @Test
    fun `login fails with invalid password format`() = testAuthApplication { client ->
        val request = LoginRequest("test@example.com", "weak")
        val response = client.postLogin(request)

        assertEquals(LoginResult.INVALID_CREDENTIALS, response.result)
        assertNull(response.accessToken)
    }

    @Test
    fun `login creates refresh token session in repository`() = testAuthApplication { client ->
        val email = "test@example.com"
        val password = "Password123!"
        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        authRepository.addUser(User("user-1", email, "testuser", passwordHash, true))

        val request = LoginRequest(email, password)
        val response = client.postLogin(request)

        assertEquals(LoginResult.SUCCESS, response.result)
        assertNotNull(response.refreshToken)

        // Verify session was created in repository
        val tokenHash = eric.bitria.hexon.utils.TokenHasher.hash(response.refreshToken!!)
        val hasSession = authRepository.hasRefreshTokenHash(tokenHash)
        assertEquals(true, hasSession, "Refresh token session should be stored in repository")
    }
}
