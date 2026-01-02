package eric.bitria.auth

import eric.bitria.auth.mock.MockRegisterRepository
import eric.bitria.auth.register.RegisterService
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication

/**
 * Launch a test server with all auth routes and JSON configured,
 * and provides a ready-to-use Ktor client.
 */
fun withTestAuthClient(block: suspend (HttpClient) -> Unit) {
    testApplication {
        application {
            configureSerialization()
            configureAuthRoutes(
                registerService = RegisterService(MockRegisterRepository())
            )
        }

        val client = createClient {
            install(ContentNegotiation) {
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
