package eric.bitria.hexon.auth.test

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eric.bitria.hexon.auth.mock.*
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.dtos.auth.*
import eric.bitria.hexon.email.mock.MockEmailVerificationService
import eric.bitria.hexon.routes.authRoutes
import eric.bitria.hexon.routes.usersRoutes
import eric.bitria.hexon.security.UserSession
import eric.bitria.hexon.services.auth.login.LoginService
import eric.bitria.hexon.services.auth.logout.LogoutService
import eric.bitria.hexon.services.auth.refresh.RefreshService
import eric.bitria.hexon.services.auth.register.RegisterService
import eric.bitria.hexon.services.email.verification.EmailVerificationService
import eric.bitria.hexon.services.users.account.UserAccountService
import eric.bitria.hexon.services.users.profile.UserProfileService
import eric.bitria.hexon.services.users.verify.AccountVerificationService
import eric.bitria.hexon.users.mock.MockAccountVerificationService
import eric.bitria.hexon.users.mock.MockUserAccountService
import eric.bitria.hexon.users.mock.MockUserProfileService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.HttpCookies
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
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

class AuthFlowTest {

    private val authRepository = MockAuthRepository()
    private val tokenService = MockTokenService()
    private val emailVerificationService = MockEmailVerificationService()

    private val registerService = MockRegisterService(authRepository, emailVerificationService)
    private val loginService = MockLoginService(authRepository, tokenService)
    private val refreshService = MockRefreshService(authRepository, tokenService)
    private val accountVerificationService = MockAccountVerificationService(
        authRepository, emailVerificationService, tokenService
    )
    private val passwordService = MockUserAccountService(authRepository, emailVerificationService)
    private val userProfileService = MockUserProfileService()
    private val logoutService = object : LogoutService {
        override suspend fun logout(refreshToken: String, request: LogoutRequest): LogoutResponse {
            authRepository.revokeRefreshToken(eric.bitria.hexon.utils.TokenHasher.hash(refreshToken))
            return LogoutResponse(LogoutResult.SUCCESS, "Logged out")
        }
    }

    private fun testAuthApplication(block: suspend (HttpClient) -> Unit) = testApplication {
        install(Koin) {
            modules(module {
                single<RegisterService> { registerService }
                single<LoginService> { loginService }
                single<RefreshService> { refreshService }
                single<AccountVerificationService> { accountVerificationService }
                single<UserAccountService> { passwordService }
                single<UserProfileService> { userProfileService }
                single<EmailVerificationService> { emailVerificationService }
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
        install(Authentication) {
            jwt {
                verifier(JWT.require(Algorithm.HMAC256("test-secret")).withIssuer("hexon-test").build())
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
            authRoutes()
            usersRoutes()
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
        val sentEmail = emailVerificationService.getSmtpService().getLastEmailTo(email)
        assertNotNull(sentEmail, "Email was not sent during registration")
        val code = sentEmail!!.body.substringAfter(": ").trim()

        val verifyResp: VerifyEmailResponse = client.post("/users/email/confirm") {
            contentType(ContentType.Application.Json)
            setBody(VerifyEmailRequest(email, code))
        }.body()
        assertEquals(VerifyEmailResult.SUCCESS, verifyResp.result)
        val initialAccessToken = verifyResp.accessToken
        assertNotNull(initialAccessToken, "Access token missing after verification")

        // 4. Login
        val loginRespSuccess: LoginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()
        assertEquals(LoginResult.SUCCESS, loginRespSuccess.result)
        assertNotNull(loginRespSuccess.accessToken, "Access token missing after login")

        // 5. Refresh Token (uses cookie set in login)
        val refreshResp: RefreshResponse = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest("")) // Body is ignored now, but must be valid JSON if expected
        }.body()
        assertEquals(RefreshResult.SUCCESS, refreshResp.result)
        val secondAccessToken = refreshResp.accessToken
        assertNotNull(secondAccessToken, "Access token missing after refresh")

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
        
        // 9. Old Refresh Token (in cookie) should be invalid after password change 
        // because password change revokes all sessions. 
        // We need to make sure the cookie used here is the OLD one or we clear it.
        // Actually, the loginNewSuccess set a NEW cookie.
        // To test revocation, we'd need to try refreshing with the cookie from BEFORE step 8.
    }
}
