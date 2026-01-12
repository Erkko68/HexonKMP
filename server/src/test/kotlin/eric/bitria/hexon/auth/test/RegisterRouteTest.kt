package eric.bitria.hexon.auth.test

import eric.bitria.hexon.services.auth.login.LoginService
import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockLoginService
import eric.bitria.hexon.auth.mock.MockRefreshService
import eric.bitria.hexon.auth.mock.MockRegisterService
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.services.auth.refresh.RefreshService
import eric.bitria.hexon.services.auth.register.RegisterService
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.repository.User
import eric.bitria.hexon.services.auth.token.TokenService
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse
import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.routes.authRoutes
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

class RegisterRouteTest {

    private val authRepository = MockAuthRepository()
    private val tokenService = MockTokenService()
    private val registerService = MockRegisterService(authRepository)

    private fun testAuthApplication(block: suspend (HttpClient) -> Unit) = testApplication {
        install(Koin) {
            modules(module {
                single<AuthRepository> { authRepository }
                single<TokenService> { tokenService }
                single<RegisterService> { registerService }
                single<LoginService> { MockLoginService(authRepository) }
                single<RefreshService> { MockRefreshService(authRepository, tokenService) }
            })
        }
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
        routing {
            authRoutes()
        }
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
        block(client)
    }

    private suspend fun HttpClient.postRegister(email: String, username: String, pass: String): RegisterResponse {
        val response = post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, username, pass))
        }
        return response.body()
    }

    @Test
    fun `registration success`() = testAuthApplication { client ->
        val email = "new@example.com"
        val response = client.postRegister(email, "newuser", "Password123!")
        
        assertEquals(RegisterResult.SUCCESS, response.result)
        assertNotNull(authRepository.findUserByEmail(email))
    }

    @Test
    fun `registration returns username taken`() = testAuthApplication { client ->
        authRepository.addUser(User("1", "old@example.com", "takenuser", "hash", true, null))
        
        val response = client.postRegister("new@example.com", "takenuser", "Password123!")
        assertEquals(RegisterResult.USERNAME_ALREADY_EXISTS, response.result)
    }

    @Test
    fun `registration returns email taken`() = testAuthApplication { client ->
        authRepository.addUser(User("1", "taken@example.com", "user1", "hash", true, null))
        
        val response = client.postRegister("taken@example.com", "user2", "Password123!")
        assertEquals(RegisterResult.EMAIL_ALREADY_EXISTS, response.result)
    }

    @Test
    fun `registration returns invalid password`() = testAuthApplication { client ->
        val response = client.postRegister("new@example.com", "newuser", "weak")
        assertEquals(RegisterResult.INVALID_PASSWORD, response.result)
    }

    @Test
    fun `registration returns invalid email format`() = testAuthApplication { client ->
        val response = client.postRegister("invalid-email", "newuser", "Password123!")
        assertEquals(RegisterResult.INVALID_EMAIL, response.result)
    }

    @Test
    fun `registration fails with malformed json`() = testApplication {
        install(Koin) {
            modules(module {
                single<RegisterService> { MockRegisterService(MockAuthRepository()) }
                single<LoginService> { MockLoginService(MockAuthRepository()) }
                single<RefreshService> { MockRefreshService(MockAuthRepository(), MockTokenService()) }
            })
        }
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
        routing {
            authRoutes()
        }
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("{ \"username\": \"test\", \"password\": }") 
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
