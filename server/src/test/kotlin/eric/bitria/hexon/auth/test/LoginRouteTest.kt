package eric.bitria.hexon.auth.test

import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockLoginService
import eric.bitria.hexon.auth.mock.MockRefreshService
import eric.bitria.hexon.auth.mock.MockRegisterService
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.routes.authRoutes
import eric.bitria.hexon.services.auth.login.LoginService
import eric.bitria.hexon.services.auth.refresh.RefreshService
import eric.bitria.hexon.services.auth.register.RegisterService
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.repository.User
import eric.bitria.hexon.services.auth.token.TokenService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

class LoginRouteTest {

    private val authRepository = MockAuthRepository()
    private val tokenService = MockTokenService()
    private val loginService = MockLoginService(authRepository)

    private fun testAuthApplication(block: suspend (HttpClient) -> Unit) = testApplication {
        install(Koin) {
            modules(module {
                single<AuthRepository> { authRepository }
                single<TokenService> { tokenService }
                single<RegisterService> { MockRegisterService(authRepository) }
                single<LoginService> { loginService }
                single<RefreshService> { MockRefreshService(authRepository, tokenService) }
            })
        }
        install(ContentNegotiation) {
            json()
        }
        routing {
            authRoutes()
        }
        val client = createClient {
            install(ClientContentNegotiation) {
                json()
            }
        }
        block(client)
    }

    private suspend fun HttpClient.postLogin(request: Any): LoginResponse {
        val response = post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body()
    }

    @Test
    fun `login success with valid credentials`() = testAuthApplication { client ->
        val email = "test@example.com"
        val password = "Password123!"
        authRepository.addUser(User("user-1", email, "testuser", password, true, null))

        val request = LoginRequest(email, password)
        val response = client.postLogin(request)

        assertEquals(LoginResult.SUCCESS, response.result)
        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)
    }

    @Test
    fun `login fails with wrong password`() = testAuthApplication { client ->
        val email = "test@example.com"
        authRepository.addUser(User("user-1", email, "testuser", "CorrectPassword123!", true, null))

        val request = LoginRequest(email, "WrongPassword123!")
        val response = client.postLogin(request)

        assertEquals(LoginResult.INVALID_CREDENTIALS, response.result)
    }

    @Test
    fun `login fails with unverified account`() = testAuthApplication { client ->
        val email = "unverified@example.com"
        authRepository.addUser(User("user-2", email, "unverified", "Password123!", false, null))

        val request = LoginRequest(email, "Password123!")
        val response = client.postLogin(request)

        assertEquals(LoginResult.NOT_VERIFIED, response.result)
    }

    @Test
    fun `login fails with non-existent user`() = testAuthApplication { client ->
        val request = LoginRequest("ghost@example.com", "Password123!")
        val response = client.postLogin(request)

        assertEquals(LoginResult.INVALID_CREDENTIALS, response.result)
    }

    @Test
    fun `login fails with invalid email format`() = testAuthApplication { client ->
        val request = LoginRequest("invalid-email", "Password123!")
        val response = client.postLogin(request)

        assertEquals(LoginResult.INVALID_CREDENTIALS, response.result)
    }

    @Test
    fun `login fails with empty fields`() = testAuthApplication { client ->
        val request = LoginRequest("", "")
        val response = client.postLogin(request)

        assertEquals(LoginResult.INVALID_CREDENTIALS, response.result)
    }

    @Test
    fun `login fails with malformed json`() = testApplication {
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
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("{ \"email\": \"test@example.com\", \"password\": }") 
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `login fails with missing password field in json`() = testApplication {
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
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("{ \"email\": \"test@example.com\" }") 
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
