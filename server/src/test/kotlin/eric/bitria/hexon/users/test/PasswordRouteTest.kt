package eric.bitria.hexon.users.test

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.auth.repository.User
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordResponse
import eric.bitria.hexon.dtos.account.ForgotPasswordResult
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordResponse
import eric.bitria.hexon.dtos.account.ResetPasswordResult
import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.email.mock.MockEmailVerificationRepository
import eric.bitria.hexon.email.mock.MockSmtpService
import eric.bitria.hexon.email.mock.MockUserRepository
import eric.bitria.hexon.email.verification.EmailVerificationServiceImpl
import eric.bitria.hexon.routes.usersRoutes
import eric.bitria.hexon.users.mock.MockAccountVerificationService
import eric.bitria.hexon.users.mock.MockPasswordService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PasswordRouteTest {

    private val authRepository = MockAuthRepository()
    private val tokenService = MockTokenService()
    
    private val smtpService = MockSmtpService()
    private val emailVerificationRepo = MockEmailVerificationRepository()
    private val emailService = EmailVerificationServiceImpl(
        emailVerificationRepo,
        smtpService,
        MockUserRepository()
    )

    private val passwordService = MockPasswordService(authRepository, emailService)

    private fun testPasswordApplication(block: suspend (HttpClient) -> Unit) = testApplication {
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
        install(Authentication) {
            jwt {
                verifier(JWT.require(Algorithm.HMAC256("secret")).build())
                validate { credential ->
                    if (credential.payload.subject != null) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
            }
        }
        routing {
            usersRoutes(
                accountVerificationService = MockAccountVerificationService(authRepository, emailService, tokenService),
                passwordService = passwordService
            )
        }
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
        block(client)
    }

    private fun generateTestToken(userId: String): String {
        return JWT.create()
            .withSubject(userId)
            .sign(Algorithm.HMAC256("secret"))
    }

    // --- CHANGE PASSWORD ---

    @Test
    fun `change password success`() = testPasswordApplication { client ->
        val userId = "u1"
        authRepository.addUser(User(userId, "test@example.com", "user", "OldPass123!", true, "token-hash"))

        val response: ChangePasswordResponse = client.post("/users/password/change") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest("OldPass123!", "NewPass123!"))
        }.body()

        assertEquals(ChangePasswordResult.SUCCESS, response.result)
        assertEquals("NewPass123!", authRepository.findUserById(userId)?.password)
        assertNull(authRepository.getRefreshTokenHash(userId))
    }

    @Test
    fun `change password fails with wrong old password`() = testPasswordApplication { client ->
        val userId = "u1"
        authRepository.addUser(User(userId, "test@example.com", "user", "CorrectPass", true, null))

        val response: ChangePasswordResponse = client.post("/users/password/change") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest("WrongPass", "NewPass"))
        }.body()

        assertEquals(ChangePasswordResult.WRONG_PASSWORD, response.result)
    }

    @Test
    fun `change password fails if user not found`() = testPasswordApplication { client ->
        val response: ChangePasswordResponse = client.post("/users/password/change") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken("ghost")}")
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest("any", "any"))
        }.body()

        assertEquals(ChangePasswordResult.USER_NOT_FOUND, response.result)
    }

    @Test
    fun `change password fails if weak password`() = testPasswordApplication { client ->
        val userId = "u1"
        authRepository.addUser(User(userId, "t@e.com", "u", "Old123", true, null))

        val response: ChangePasswordResponse = client.post("/users/password/change") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest("Old123", "weak"))
        }.body()

        assertEquals(ChangePasswordResult.INVALID_PASSWORD, response.result)
    }

    @Test
    fun `change password fails if unauthorized`() = testPasswordApplication { client ->
        val response = client.post("/users/password/change") {
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest("old", "new"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // --- FORGOT PASSWORD ---

    @Test
    fun `forgot password triggers reset code email for existing user`() = testPasswordApplication { client ->
        val email = "reset@example.com"
        authRepository.addUser(User("u1", email, "user", "pass", true, null))

        val response: ForgotPasswordResponse = client.post("/users/password/forgot") {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest(email))
        }.body()

        assertEquals(ForgotPasswordResult.SUCCESS, response.result)
        assertNotNull(smtpService.getLastEmailTo(email))
    }

    @Test
    fun `forgot password returns success even if user not found (security)`() = testPasswordApplication { client ->
        val response: ForgotPasswordResponse = client.post("/users/password/forgot") {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest("ghost@example.com"))
        }.body()

        assertEquals(ForgotPasswordResult.SUCCESS, response.result)
        assertNull(smtpService.getLastEmailTo("ghost@example.com"))
    }

    // --- RESET PASSWORD ---

    @Test
    fun `reset password success with valid code`() = testPasswordApplication { client ->
        val email = "reset@example.com"
        val userId = "u1"
        authRepository.addUser(User(userId, email, "user", "OldPass", true, "active-session"))

        emailService.sendVerificationCodeByEmail(email, EmailVerificationType.PASSWORD_RESET)
        val code = smtpService.getLastEmailTo(email)!!.body.substringAfter(": ").trim()

        val response: ResetPasswordResponse = client.post("/users/password/reset") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(email, code, "NewPass123!"))
        }.body()

        assertEquals(ResetPasswordResult.SUCCESS, response.result)
        assertEquals("NewPass123!", authRepository.findUserByEmail(email)?.password)
        assertNull(authRepository.getRefreshTokenHash(userId))
    }

    @Test
    fun `reset password fails with invalid code`() = testPasswordApplication { client ->
        val email = "test@example.com"
        authRepository.addUser(User("u1", email, "user", "pass", true, null))

        val response: ResetPasswordResponse = client.post("/users/password/reset") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(email, "000000", "NewPass"))
        }.body()

        assertEquals(ResetPasswordResult.INVALID_CODE, response.result)
    }

    @Test
    fun `reset password fails if user disappeared`() = testPasswordApplication { client ->
        val email = "vanish@example.com"
        emailService.sendVerificationCodeByEmail(email, EmailVerificationType.PASSWORD_RESET)
        val code = smtpService.getLastEmailTo(email)!!.body.substringAfter(": ").trim()

        val response: ResetPasswordResponse = client.post("/users/password/reset") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(email, code, "NewPass"))
        }.body()

        assertEquals(ResetPasswordResult.USER_NOT_FOUND, response.result)
    }
}
