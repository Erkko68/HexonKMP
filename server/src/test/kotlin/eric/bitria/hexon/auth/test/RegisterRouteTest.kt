package eric.bitria.hexon.auth.test

import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse
import eric.bitria.hexon.dtos.auth.RegisterResult
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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
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

class RegisterRouteTest {

    private val authRepository = MockAuthRepository()
    private val emailVerificationRepository = MockEmailVerificationRepository()
    private val smtpService = MockSmtpService()
    private val tokenService = MockTokenService()

    private val emailVerificationService = EmailVerificationServiceImpl(
        emailVerificationRepository,
        smtpService,
        authRepository
    )

    private val registerService = RegisterServiceImpl(authRepository, emailVerificationService)
    private val loginService = LoginServiceImpl(authRepository, tokenService)
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
        }
        block(client)
    }

    private suspend fun HttpClient.postRegister(email: String, username: String, pass: String): RegisterResponse {
        val response = post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, username, pass))
        }
        return response.body()
    }

    @Test
    fun `registration success`() = testAuthApplication { client ->
        val email = "new@example.com"
        val response = client.postRegister(email, "newuser", "Password123!")
        
        assertEquals(RegisterResult.SUCCESS, response.result)
        assertNotNull(authRepository.findUserByEmail(email))

        // Verify email was sent
        val sentEmail = smtpService.getLastEmailTo(email)
        assertNotNull(sentEmail, "Verification email should be sent")
        assertTrue(sentEmail!!.body.contains("verification code", ignoreCase = true))
    }

    @Test
    fun `registration returns username taken`() = testAuthApplication { client ->
        authRepository.addUser(User("1", "old@example.com", "takenuser", "hash", true))
        
        val response = client.postRegister("new@example.com", "takenuser", "Password123!")
        assertEquals(RegisterResult.USERNAME_ALREADY_EXISTS, response.result)
    }

    @Test
    fun `registration returns email taken`() = testAuthApplication { client ->
        authRepository.addUser(User("1", "taken@example.com", "user1", "hash", true))
        
        val response = client.postRegister("taken@example.com", "user2", "Password123!")
        assertEquals(RegisterResult.EMAIL_ALREADY_EXISTS, response.result)
    }

    @Test
    fun `registration returns invalid password`() = testAuthApplication { client ->
        val response = client.postRegister("new@example.com", "newuser", "weak")
        assertEquals(RegisterResult.INVALID_PASSWORD, response.result)
    }

    @Test
    fun `registration returns invalid email format`() = testAuthApplication { client ->
        val response = client.postRegister("invalid-email", "newuser", "Password123!")
        assertEquals(RegisterResult.INVALID_EMAIL, response.result)
    }

    @Test
    fun `registration returns invalid username - too short`() = testAuthApplication { client ->
        val response = client.postRegister("test@example.com", "ab", "Password123!")
        assertEquals(RegisterResult.INVALID_USERNAME, response.result)
    }

    @Test
    fun `registration returns invalid username - contains special characters`() = testAuthApplication { client ->
        val response = client.postRegister("test@example.com", "user@name", "Password123!")
        assertEquals(RegisterResult.INVALID_USERNAME, response.result)
    }

    @Test
    fun `registration returns invalid password - too short`() = testAuthApplication { client ->
        val response = client.postRegister("test@example.com", "newuser", "Pass1!")
        assertEquals(RegisterResult.INVALID_PASSWORD, response.result)
    }

    @Test
    fun `registration returns invalid password - no uppercase`() = testAuthApplication { client ->
        val response = client.postRegister("test@example.com", "newuser", "password123!")
        assertEquals(RegisterResult.INVALID_PASSWORD, response.result)
    }

    @Test
    fun `registration returns invalid password - no number`() = testAuthApplication { client ->
        val response = client.postRegister("test@example.com", "newuser", "Password!")
        assertEquals(RegisterResult.INVALID_PASSWORD, response.result)
    }
}
