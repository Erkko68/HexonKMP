package eric.bitria.hexon.users.test

import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResponse
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResult
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import eric.bitria.hexon.email.mock.MockEmailVerificationService
import eric.bitria.hexon.routes.usersRoutes
import eric.bitria.hexon.security.UserSession
import eric.bitria.hexon.services.auth.repository.User
import eric.bitria.hexon.services.users.account.UserAccountService
import eric.bitria.hexon.services.users.profile.UserProfileService
import eric.bitria.hexon.services.users.verify.AccountVerificationService
import eric.bitria.hexon.users.mock.MockAccountVerificationService
import eric.bitria.hexon.users.mock.MockUserAccountService
import eric.bitria.hexon.users.mock.MockUserProfileService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

class AccountVerificationRouteTest {

    private val authRepository = MockAuthRepository()
    private val tokenService = MockTokenService()
    private val emailService = MockEmailVerificationService()

    private val accountVerificationService = MockAccountVerificationService(
        authRepository,
        emailService,
        tokenService
    )

    private val userProfileService = MockUserProfileService()

    private fun testUsersApplication(block: suspend (HttpClient) -> Unit) = testApplication {
        install(Koin) {
            modules(module {
                single<AccountVerificationService> { accountVerificationService }
                single<UserAccountService> { MockUserAccountService(authRepository, emailService) }
                single<UserProfileService> { userProfileService }
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
        install(Authentication) {
            jwt {
                validate { null }
            }
        }
        routing {
            usersRoutes()
        }
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
        block(client)
    }

    @Test
    fun `verify email success with correct code`() = testUsersApplication { client ->
        val email = "verify@example.com"
        authRepository.addUser(User("u1", email, "user", "pass", false))

        // 1. Trigger code generation
        emailService.sendVerificationCodeByEmail(email, EmailVerificationType.EMAIL_CONFIRMATION)
        val sentEmail = emailService.getSmtpService().getLastEmailTo(email)
        val code = sentEmail!!.body.substringAfter(": ").trim()

        // 2. Call verification endpoint
        val response: VerifyEmailResponse = client.post("/users/email/confirm") {
            contentType(ContentType.Application.Json)
            setBody(VerifyEmailRequest(email, code))
        }.body()

        assertEquals(VerifyEmailResult.SUCCESS, response.result)
        assertNotNull(response.accessToken)
        
        // 3. Verify state: User is now verified in DB
        assertTrue(authRepository.findUserByEmail(email)!!.isVerified)
    }

    @Test
    fun `verify email fails with wrong code`() = testUsersApplication { client ->
        val email = "wrong@example.com"
        authRepository.addUser(User("u1", email, "user", "pass", false))
        emailService.sendVerificationCodeByEmail(email, EmailVerificationType.EMAIL_CONFIRMATION)

        val response: VerifyEmailResponse = client.post("/users/email/confirm") {
            contentType(ContentType.Application.Json)
            setBody(VerifyEmailRequest(email, "000000"))
        }.body()

        assertEquals(VerifyEmailResult.INVALID_CODE, response.result)
        assertFalse(authRepository.findUserByEmail(email)!!.isVerified)
    }

    @Test
    fun `resend verification code sends a new email`() = testUsersApplication { client ->
        val email = "resend@example.com"
        authRepository.addUser(User("u1", email, "user", "pass", false))

        val response: ResendVerificationCodeResponse = client.post("/users/email/resend") {
            contentType(ContentType.Application.Json)
            setBody(ResendVerificationCodeRequest(email))
        }.body()

        assertEquals(ResendVerificationCodeResult.SUCCESS, response.result)
        
        val sentEmail = emailService.getSmtpService().getLastEmailTo(email)
        assertNotNull(sentEmail)
        assertTrue(sentEmail!!.body.contains("Your verification code is:"))
    }

    @Test
    fun `resend verification code fails if already verified`() = testUsersApplication { client ->
        val email = "done@example.com"
        authRepository.addUser(User("u1", email, "user", "pass", true))

        val response: ResendVerificationCodeResponse = client.post("/users/email/resend") {
            contentType(ContentType.Application.Json)
            setBody(ResendVerificationCodeRequest(email))
        }.body()

        assertEquals(ResendVerificationCodeResult.ALREADY_VERIFIED, response.result)
    }

    @Test
    fun `verify email fails if user not found`() = testUsersApplication { client ->
        val response: VerifyEmailResponse = client.post("/users/email/confirm") {
            contentType(ContentType.Application.Json)
            setBody(VerifyEmailRequest("ghost@example.com", "123456"))
        }.body()

        assertEquals(VerifyEmailResult.USER_NOT_FOUND, response.result)
    }
}
