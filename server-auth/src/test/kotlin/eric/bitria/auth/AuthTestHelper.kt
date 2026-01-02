package eric.bitria.auth

import eric.bitria.auth.mock.MockRegisterRepository
import eric.bitria.auth.mock.MockTokenService
import eric.bitria.auth.register.RegisterService
import eric.bitria.auth.routes.registerRoutes
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

/**
 * Launch a test server with all auth routes and JSON configured,
 * and provides a ready-to-use Ktor client.
 */
fun withTestAuthClient(block: suspend (HttpClient) -> Unit) {
    testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing {
                registerRoutes(registerService = RegisterService(
                    repository = MockRegisterRepository(),
                    tokenService = MockTokenService()
                ))
                // loginRoutes(loginService)
            }
        }

        val client = createClient {
            install(ClientContentNegotiation) {
                json()
            }
        }

        block(client)
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