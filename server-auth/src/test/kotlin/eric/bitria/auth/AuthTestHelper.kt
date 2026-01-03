package eric.bitria.auth

import eric.bitria.auth.mock.*
import eric.bitria.auth.routes.loginRoute
import eric.bitria.auth.routes.refreshRoute
import eric.bitria.auth.routes.registerRoutes
import eric.bitria.hexon.dtos.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

/**
 * Launch a test server with all auth routes and JSON configured,
 * and provides a ready-to-use Ktor client.
 */
fun withTestAuthClient(
    block: suspend (HttpClient, Inbox) -> Unit
) {
    val inbox = Inbox("")
    val tokenService = MockTokenService()
    val repository = MockAuthRepository()

    testApplication {
        application {
            install(ContentNegotiation) { json() }

            routing {
                registerRoutes(
                    registerService = MockRegisterService(
                        repository = repository,
                        tokenService = tokenService,
                        emailService = MockEmailService(inbox)
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
                        tokenService = tokenService
                    )
                )
            }
        }

        val client = createClient {
            install(ClientContentNegotiation) { json() }
        }

        block(client, inbox)
    }
}

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

/**
 * Abbreviation of the resend verification endpoint.
 */
suspend fun HttpClient.resendVerification(
    email: String
): ResendVerificationCodeResponse = post("/auth/resend-verification") {
    contentType(ContentType.Application.Json)
    setBody(ResendVerificationCodeRequest(email))
}.body()

/**
 * Abbreviation of the refresh endpoint.
 */
suspend fun HttpClient.refresh(
    refreshToken: String
): RefreshResponse = post("/auth/refresh") {
    contentType(ContentType.Application.Json)
    setBody(RefreshRequest(refreshToken))
}.body()

/**
 * Abbreviation of the login endpoint.
 */
suspend fun HttpClient.login(
    email: String,
    password: String
): LoginResponse = post("/auth/login") {
    contentType(ContentType.Application.Json)
    setBody(LoginRequest(email, password))
}.body()
