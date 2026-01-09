package eric.bitria.hexon.account

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eric.bitria.hexon.auth.mock.Inbox
import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockEmailService
import eric.bitria.hexon.auth.mock.MockLoginService
import eric.bitria.hexon.auth.mock.MockPasswordService
import eric.bitria.hexon.auth.mock.MockRefreshService
import eric.bitria.hexon.auth.mock.MockRegisterService
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.auth.token.JwtConfig
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordResponse
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse
import eric.bitria.hexon.routes.accountRoutes
import eric.bitria.hexon.routes.loginRoute
import eric.bitria.hexon.routes.refreshRoute
import eric.bitria.hexon.routes.registerRoutes
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

/**
 * Launch a test server with account routes and JSON configured,
 * and provides a ready-to-use Ktor client.
 */
fun withTestAccountClient(
    block: suspend (HttpClient, Inbox) -> Unit
) {
    val inbox = Inbox("")
    val tokenService = MockTokenService()
    val emailService = MockEmailService(inbox)
    val repository = MockAuthRepository()
    
    val testJwtConfig = JwtConfig(
        issuer = "test-issuer",
        audience = "test-audience",
        secret = "test-secret",
        realm = "test-realm",
        accessTokenTtlMillis = 3600000,
        refreshTokenTtlMillis = 3600000
    )

    testApplication {
        application {
            install(ContentNegotiation) { json() }
            
            authentication {
                jwt("auth-jwt") {
                    realm = testJwtConfig.realm
                    verifier(
                        JWT.require(Algorithm.HMAC256(testJwtConfig.secret))
                            .withIssuer(testJwtConfig.issuer)
                            .withAudience(testJwtConfig.audience)
                            .build()
                    )
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
                registerRoutes(
                    registerService = MockRegisterService(
                        repository = repository,
                        tokenService = tokenService,
                        emailService = emailService
                    )
                )
                refreshRoute(
                    refreshService = MockRefreshService(
                        tokenService = tokenService
                    )
                )
                loginRoute(
                    loginService = MockLoginService(
                        repository = repository,
                        tokenService = tokenService,
                        emailService = emailService
                    )
                )
                accountRoutes(
                    passwordService = MockPasswordService(
                        repository = repository,
                        emailService = emailService
                    )
                )
            }
        }

        val client = createClient {
            install(ClientContentNegotiation) { json() }
            install(Auth) {
                bearer {
                    loadTokens {
                        // For tests, we use a dummy token that will be validated by the mock verifier
                        BearerTokens(
                            accessToken = JWT.create()
                                .withSubject("test-user")
                                .withIssuer(testJwtConfig.issuer)
                                .withAudience(testJwtConfig.audience)
                                .sign(Algorithm.HMAC256(testJwtConfig.secret)),
                            refreshToken = "test-refresh-token"
                        )
                    }
                }
            }
        }

        block(client, inbox)
    }
}

suspend fun HttpClient.forgotPassword(
    email: String
): ForgotPasswordResponse = post("/account/forgot-password") {
    contentType(ContentType.Application.Json)
    setBody(ForgotPasswordRequest(email))
}.body()

suspend fun HttpClient.changePassword(
    email: String,
    resetCode: String? = null,
    newPassword: String,
    oldPassword: String? = null
): ChangePasswordResponse = post("/account/change-password") {
    contentType(ContentType.Application.Json)
    setBody(ChangePasswordRequest(email, resetCode, oldPassword, newPassword))
}.body()


// Auxiliary functions


/**
 * Abbreviation of the register endpoint.
 */
suspend fun HttpClient.register(
    username: String,
    email: String,
    password: String
): RegisterResponse = post("/auth/register") {
    contentType(ContentType.Application.Json)
    setBody(RegisterRequest(username, email, password))
}.body()

/**
 * Abbreviation of the verify endpoint.
 */
suspend fun HttpClient.verify(
    email: String,
    code: String
): VerifyEmailResponse = post("/auth/verify") {
    contentType(ContentType.Application.Json)
    setBody(VerifyEmailRequest(email, code))
}.body()
