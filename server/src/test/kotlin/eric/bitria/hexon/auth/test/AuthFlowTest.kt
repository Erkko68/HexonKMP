package eric.bitria.hexon.auth.test

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockLoginService
import eric.bitria.hexon.auth.mock.MockRefreshService
import eric.bitria.hexon.auth.mock.MockRegisterService
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse
import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import eric.bitria.hexon.email.mock.MockEmailVerificationRepository
import eric.bitria.hexon.email.mock.MockSmtpService
import eric.bitria.hexon.email.verification.EmailVerificationServiceImpl
import eric.bitria.hexon.routes.authRoutes
import eric.bitria.hexon.routes.usersRoutes
import eric.bitria.hexon.users.mock.MockAccountVerificationService
import eric.bitria.hexon.users.mock.MockUserAccountService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AuthFlowTest {

    private val authRepository = MockAuthRepository()
    private val tokenService = MockTokenService()
    
    // Email infrastructure
    private val smtpService = MockSmtpService()
    private val emailVerificationRepo = MockEmailVerificationRepository()
    private val emailVerificationService = EmailVerificationServiceImpl(
        emailVerificationRepo,
        smtpService,
        authRepository
    )

    // Services - Important to pass emailVerificationService to RegisterService
    private val registerService = MockRegisterService(authRepository, emailVerificationService)
    private val loginService = MockLoginService(authRepository)
    private val refreshService = MockRefreshService(authRepository, tokenService)
    private val accountVerificationService = MockAccountVerificationService(
        authRepository, emailVerificationService, tokenService
    )
    private val passwordService = MockUserAccountService(authRepository, emailVerificationService)

    private fun testAuthApplication(block: suspend (HttpClient) -> Unit) = testApplication {
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
            authRoutes(registerService, loginService, refreshService)
            usersRoutes(accountVerificationService, passwordService)
        }
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
        block(client)
    }

    @Test
    fun `complete user lifecycle - register, verify, login, refresh, change password, login with new`() = testAuthApplication { client ->
        val email = "full-flow@example.com"
        val password = "InitialPassword123!"
        val username = "fullflowuser"

        // 1. Register
        val regResp: RegisterResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, username, password))
        }.body()
        assertEquals(RegisterResult.SUCCESS, regResp.result)

        // 2. Try Login -> Should fail (NOT_VERIFIED)
        val loginRespFail: LoginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()
        assertEquals(LoginResult.NOT_VERIFIED, loginRespFail.result)

        // 3. Verify Email
        val sentEmail = smtpService.getLastEmailTo(email)
        assertNotNull(sentEmail, "Email was not sent during registration")
        val code = sentEmail!!.body.substringAfter(": ").trim()

        val verifyResp: VerifyEmailResponse = client.post("/users/email/confirm") {
            contentType(ContentType.Application.Json)
            setBody(VerifyEmailRequest(email, code))
        }.body()
        assertEquals(VerifyEmailResult.SUCCESS, verifyResp.result)
        val initialAccessToken = verifyResp.accessToken
        val initialRefreshToken = verifyResp.refreshToken
        assertNotNull(initialAccessToken, "Access token missing after verification")
        assertNotNull(initialRefreshToken, "Refresh token missing after verification")

        // 4. Login
        val loginRespSuccess: LoginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()
        assertEquals(LoginResult.SUCCESS, loginRespSuccess.result)
        assertNotNull(loginRespSuccess.accessToken, "Access token missing after login")

        // 5. Refresh Token
        val refreshResp: RefreshResponse = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(initialRefreshToken!!))
        }.body()
        assertEquals(RefreshResult.SUCCESS, refreshResp.result)
        val secondAccessToken = refreshResp.accessToken
        val secondRefreshToken = refreshResp.refreshToken
        assertNotNull(secondAccessToken, "Access token missing after refresh")
        assertNotNull(secondRefreshToken, "Refresh token missing after refresh")

        // 6. Change Password
        val newPassword = "UpdatedPassword456!"
        val changePasswordResp: ChangePasswordResponse = client.post("/users/password/change") {
            header(HttpHeaders.Authorization, "Bearer $secondAccessToken")
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest(password, newPassword))
        }.body()
        assertEquals(ChangePasswordResult.SUCCESS, changePasswordResp.result)

        // 7. Try Login with Old Password -> Should fail
        val loginOldFail: LoginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()
        assertEquals(LoginResult.INVALID_CREDENTIALS, loginOldFail.result)

        // 8. Try Login with New Password -> Should succeed
        val loginNewSuccess: LoginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, newPassword))
        }.body()
        assertEquals(LoginResult.SUCCESS, loginNewSuccess.result)
        
        // 9. Old Refresh Token should be revoked after password change
        val refreshOldFail: RefreshResponse = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(secondRefreshToken!!))
        }.body()
        assertEquals(RefreshResult.INVALID_TOKEN, refreshOldFail.result)
    }

    @Test
    fun `cannot login with incorrect credentials even after verification`() = testAuthApplication { client ->
        val email = "safety@example.com"
        val password = "Password123!"
        
        val regResp: RegisterResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, "safetyuser", password))
        }.body()
        assertEquals(RegisterResult.SUCCESS, regResp.result)
        
        val sentEmail = smtpService.getLastEmailTo(email)
        assertNotNull(sentEmail, "Email was not sent for safety test")
        val code = sentEmail!!.body.substringAfter(": ").trim()
        
        val verifyResp: VerifyEmailResponse = client.post("/users/email/confirm") {
            contentType(ContentType.Application.Json)
            setBody(VerifyEmailRequest(email, code))
        }.body()
        assertEquals(VerifyEmailResult.SUCCESS, verifyResp.result)

        val loginResp: LoginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, "WrongPassword!"))
        }.body()
        assertEquals(LoginResult.INVALID_CREDENTIALS, loginResp.result)
    }
}
