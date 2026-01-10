package eric.bitria.hexon.users.test

import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.auth.repository.User
import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResponse
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResult
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import eric.bitria.hexon.email.mock.MockEmailVerificationRepository
import eric.bitria.hexon.email.mock.MockSmtpService
import eric.bitria.hexon.email.mock.MockUserRepository
import eric.bitria.hexon.email.verification.EmailVerificationServiceImpl
import eric.bitria.hexon.routes.usersRoutes
import eric.bitria.hexon.users.mock.MockAccountVerificationService
import eric.bitria.hexon.users.mock.MockUserAccountService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountVerificationRouteTest {

    private val authRepository = MockAuthRepository()
    private val tokenService = MockTokenService()
    
    // We need a real-ish implementation for email logic to capture the code
    private val smtpService = MockSmtpService()
    private val emailVerificationRepo = MockEmailVerificationRepository()
    private val emailService = EmailVerificationServiceImpl(
        emailVerificationRepo,
        smtpService,
        MockUserRepository()
    )

    private val accountVerificationService = MockAccountVerificationService(
        authRepository,
        emailService,
        tokenService
    )

    private fun testUsersApplication(block: suspend (HttpClient) -> Unit) = testApplication {
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
        install(Authentication) {
            jwt {
                validate { null }
            }
        }
        routing {
            usersRoutes(
                accountVerificationService = accountVerificationService,
                userAccountService = MockUserAccountService(
                    authRepository,
                    emailService
                )
            )
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
        authRepository.addUser(User("u1", email, "user", "pass", false, null))

        // 1. Trigger code generation
        emailService.sendVerificationCodeByEmail(email, EmailVerificationType.EMAIL_CONFIRMATION)
        val sentEmail = smtpService.getLastEmailTo(email)
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
        authRepository.addUser(User("u1", email, "user", "pass", false, null))
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
        authRepository.addUser(User("u1", email, "user", "pass", false, null))

        val response: ResendVerificationCodeResponse = client.post("/users/email/resend") {
            contentType(ContentType.Application.Json)
            setBody(ResendVerificationCodeRequest(email))
        }.body()

        assertEquals(ResendVerificationCodeResult.SUCCESS, response.result)
        
        val sentEmail = smtpService.getLastEmailTo(email)
        assertNotNull(sentEmail)
        assertTrue(sentEmail!!.body.contains("Your verification code is:"))
    }

    @Test
    fun `resend verification code fails if already verified`() = testUsersApplication { client ->
        val email = "done@example.com"
        authRepository.addUser(User("u1", email, "user", "pass", true, null))

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
