package eric.bitria.hexon.users.test

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.services.auth.repository.User
import eric.bitria.hexon.dtos.account.*
import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.email.mock.MockEmailVerificationService
import eric.bitria.hexon.routes.usersRoutes
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.token.TokenService
import eric.bitria.hexon.services.email.repository.EmailVerificationRepository
import eric.bitria.hexon.services.email.smtp.SmtpService
import eric.bitria.hexon.services.email.verification.EmailVerificationService
import eric.bitria.hexon.services.users.account.UserAccountService
import eric.bitria.hexon.services.users.account.UserAccountServiceImpl
import eric.bitria.hexon.services.users.profile.ProfileRepository
import eric.bitria.hexon.services.users.profile.UserProfileService
import eric.bitria.hexon.services.users.profile.UserProfileServiceImpl
import eric.bitria.hexon.services.users.verify.AccountVerificationService
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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.time.LocalDateTime

class PasswordRouteTest {

    private val authRepository = MockAuthRepository()
    private val emailVerificationRepository = eric.bitria.hexon.email.mock.MockEmailVerificationRepository()
    private val smtpService = eric.bitria.hexon.email.mock.MockSmtpService()
    private val tokenService = MockTokenService()
    private val profileRepository = eric.bitria.hexon.users.mock.MockProfileRepository()

    private val emailVerificationService = eric.bitria.hexon.services.email.verification.EmailVerificationServiceImpl(
        emailVerificationRepository,
        smtpService,
        authRepository
    )

    private val accountVerificationService = eric.bitria.hexon.services.users.verify.AccountVerificationServiceImpl(
        authRepository,
        emailVerificationService,
        tokenService,
        profileRepository
    )

    private val userAccountService = eric.bitria.hexon.services.users.account.UserAccountServiceImpl(authRepository, emailVerificationService)
    private val userProfileService = eric.bitria.hexon.services.users.profile.UserProfileServiceImpl(profileRepository)

    private fun testPasswordApplication(block: suspend (HttpClient) -> Unit) = testApplication {
        install(Koin) {
            modules(module {
                single<AuthRepository> { authRepository }
                single<EmailVerificationRepository> { emailVerificationRepository }
                single<SmtpService> { smtpService }
                single<TokenService> { tokenService }
                single<ProfileRepository> { profileRepository }
                single<EmailVerificationService> { emailVerificationService }
                single<AccountVerificationService> { accountVerificationService }
                single<UserAccountService> { userAccountService }
                single<UserProfileService> { userProfileService }
            })
        }
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
            usersRoutes()
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
        val oldPassword = "OldPass123!"
        val oldPasswordHash = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(12, oldPassword.toCharArray())
        authRepository.addUser(User(userId, "test@example.com", "user", oldPasswordHash, true))
        // Add a mock session to verify it gets revoked
        val sessionHash = "token-hash"
        authRepository.addRefreshToken(userId, sessionHash, LocalDateTime.now().plusDays(7))

        val newPassword = "NewPass123!"
        val response: ChangePasswordResponse = client.post("/users/password/change") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest(oldPassword, newPassword))
        }.body()

        assertEquals(ChangePasswordResult.SUCCESS, response.result)

        // Verify new password is hashed correctly
        val user = authRepository.findUserById(userId)!!
        assertTrue(at.favre.lib.crypto.bcrypt.BCrypt.verifyer().verify(newPassword.toCharArray(), user.password).verified)

        // Verify session is revoked
        assertFalse(authRepository.hasRefreshTokenHash(sessionHash))
    }

    @Test
    fun `change password fails with wrong old password`() = testPasswordApplication { client ->
        val userId = "u1"
        val correctPassword = "CorrectPass123!"
        val passwordHash = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(12, correctPassword.toCharArray())
        authRepository.addUser(User(userId, "test@example.com", "user", passwordHash, true))

        val response: ChangePasswordResponse = client.post("/users/password/change") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest("WrongPass123!", "NewPass123!"))
        }.body()

        assertEquals(ChangePasswordResult.WRONG_PASSWORD, response.result)
    }

    @Test
    fun `change password fails if user not found`() = testPasswordApplication { client ->
        val response: ChangePasswordResponse = client.post("/users/password/change") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken("ghost")}")
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest("ValidPass123!", "NewPass123!"))
        }.body()

        assertEquals(ChangePasswordResult.USER_NOT_FOUND, response.result)
    }

    @Test
    fun `change password fails if weak password`() = testPasswordApplication { client ->
        val userId = "u1"
        authRepository.addUser(User(userId, "t@e.com", "u", "Old123", true))

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
        authRepository.addUser(User("u1", email, "user", "pass", true))

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
        val oldPasswordHash = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(12, "OldPass123!".toCharArray())
        authRepository.addUser(User(userId, email, "user", oldPasswordHash, true))
        // Add a mock session to verify it gets revoked
        val sessionHash = "active-session"
        authRepository.addRefreshToken(userId, sessionHash, LocalDateTime.now().plusDays(7))

        emailVerificationService.sendVerificationCodeByEmail(email, EmailVerificationType.PASSWORD_RESET)
        val sentEmail = smtpService.getLastEmailTo(email)!!
        val code = sentEmail.body.substringAfter("Your verification code is: ").trim()

        val newPassword = "NewPass123!"
        val response: ResetPasswordResponse = client.post("/users/password/reset") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(email, code, newPassword))
        }.body()

        assertEquals(ResetPasswordResult.SUCCESS, response.result)

        // Verify password was updated with BCrypt
        val user = authRepository.findUserByEmail(email)!!
        assertTrue(at.favre.lib.crypto.bcrypt.BCrypt.verifyer().verify(newPassword.toCharArray(), user.password).verified)

        // Verify session is revoked
        assertFalse(authRepository.hasRefreshTokenHash(sessionHash))
    }

    @Test
    fun `reset password fails with invalid code`() = testPasswordApplication { client ->
        val email = "test@example.com"
        val passwordHash = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(12, "OldPass123!".toCharArray())
        authRepository.addUser(User("u1", email, "user", passwordHash, true))

        val response: ResetPasswordResponse = client.post("/users/password/reset") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(email, "000000", "NewPass123!"))
        }.body()

        assertEquals(ResetPasswordResult.INVALID_CODE, response.result)
    }

    @Test
    fun `reset password fails if user disappeared`() = testPasswordApplication { client ->
        val email = "vanish@example.com"
        emailVerificationService.sendVerificationCodeByEmail(email, EmailVerificationType.PASSWORD_RESET)
        val sentEmail = smtpService.getLastEmailTo(email)!!
        val code = sentEmail.body.substringAfter("Your verification code is: ").trim()

        val response: ResetPasswordResponse = client.post("/users/password/reset") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(email, code, "NewPass123!"))
        }.body()

        assertEquals(ResetPasswordResult.USER_NOT_FOUND, response.result)
    }
}
