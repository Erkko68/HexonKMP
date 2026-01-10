package eric.bitria.hexon.users.test

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eric.bitria.hexon.auth.mock.MockAuthRepository
import eric.bitria.hexon.auth.mock.MockTokenService
import eric.bitria.hexon.auth.repository.User
import eric.bitria.hexon.dtos.account.ConfirmDeleteAccountRequest
import eric.bitria.hexon.dtos.account.ConfirmDeleteAccountResponse
import eric.bitria.hexon.dtos.account.DeleteAccountResult
import eric.bitria.hexon.dtos.account.RequestDeleteAccountResponse
import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.email.mock.MockEmailVerificationRepository
import eric.bitria.hexon.email.mock.MockSmtpService
import eric.bitria.hexon.email.verification.EmailVerificationServiceImpl
import eric.bitria.hexon.routes.usersRoutes
import eric.bitria.hexon.users.mock.MockAccountVerificationService
import eric.bitria.hexon.users.mock.MockUserAccountService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AccountDeletionRouteTest {

    private val authRepository = MockAuthRepository()
    private val tokenService = MockTokenService()

    private val smtpService = MockSmtpService()
    private val emailVerificationRepo = MockEmailVerificationRepository()
    private val emailService = EmailVerificationServiceImpl(
        emailVerificationRepo,
        smtpService,
        authRepository
    )

    private val accountService = MockUserAccountService(authRepository, emailService)

    private fun testAccountApplication(block: suspend (HttpClient) -> Unit) = testApplication {
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
                userAccountService = accountService
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

    @Test
    fun `request account deletion sends email code`() = testAccountApplication { client ->
        val userId = "u1"
        val email = "delete@example.com"
        authRepository.addUser(User(userId, email, "user", "pass", true, null))

        val response: RequestDeleteAccountResponse = client.post("/users/me/delete/initiate") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
        }.body()

        assertNotNull(response.message)
        assertNotNull(smtpService.getLastEmailTo(email))
    }

    @Test
    fun `confirm account deletion success`() = testAccountApplication { client ->
        val userId = "u1"
        val email = "delete@example.com"
        val password = "Password123!"
        authRepository.addUser(User(userId, email, "user", password, true, null))

        // Initiate to get the code
        client.post("/users/me/delete/initiate") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
        }
        val code = smtpService.getLastEmailTo(email)!!.body.substringAfter(": ").trim()

        val response: ConfirmDeleteAccountResponse = client.delete("/users/me") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(ConfirmDeleteAccountRequest(password, code))
        }.body()

        assertEquals(DeleteAccountResult.SUCCESS, response.result)
        assertNull(authRepository.findUserById(userId))
    }

    @Test
    fun `confirm account deletion fails with wrong password`() = testAccountApplication { client ->
        val userId = "u1"
        val email = "delete@example.com"
        authRepository.addUser(User(userId, email, "user", "correct", true, null))

        client.post("/users/me/delete/initiate") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
        }
        val code = smtpService.getLastEmailTo(email)!!.body.substringAfter(": ").trim()

        val response: ConfirmDeleteAccountResponse = client.delete("/users/me") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(ConfirmDeleteAccountRequest("wrong", code))
        }.body()

        assertEquals(DeleteAccountResult.WRONG_PASSWORD, response.result)
        assertNotNull(authRepository.findUserById(userId))
    }

    @Test
    fun `confirm account deletion fails with invalid code`() = testAccountApplication { client ->
        val userId = "u1"
        val email = "delete@example.com"
        authRepository.addUser(User(userId, email, "user", "pass", true, null))

        val response: ConfirmDeleteAccountResponse = client.delete("/users/me") {
            header(HttpHeaders.Authorization, "Bearer ${generateTestToken(userId)}")
            contentType(ContentType.Application.Json)
            setBody(ConfirmDeleteAccountRequest("pass", "000000"))
        }.body()

        assertEquals(DeleteAccountResult.INVALID_CODE, response.result)
        assertNotNull(authRepository.findUserById(userId))
    }
}
