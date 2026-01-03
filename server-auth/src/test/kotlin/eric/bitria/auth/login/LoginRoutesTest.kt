package eric.bitria.auth.login

import eric.bitria.auth.login
import eric.bitria.auth.register
import eric.bitria.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.LoginResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LoginRoutesTest {

    private val email = "test@example.com"
    private val password = "Password123"
    private val username = "testuser"

    @Test
    fun `login returns SUCCESS for registered user with correct password`() =
        withTestAuthClient { client, _ ->
            client.register(username, email, password)

            val response = client.login(email, password)

            assertEquals(LoginResult.SUCCESS, response.result)
            assertNotNull(response.accessToken)
            assertTrue(response.accessToken.isNotEmpty())
            assertNotNull(response.refreshToken)
            assertTrue(response.refreshToken.isNotEmpty())
        }

    @Test
    fun `login returns INVALID_EMAIL_OR_PASSWORD for non-registered email`() =
        withTestAuthClient { client, _ ->
            val response = client.login("nonexistent@example.com", password)

            assertEquals(LoginResult.INVALID_EMAIL_OR_PASSWORD, response.result)
            assertTrue(response.accessToken.isEmpty())
        }

    @Test
    fun `login returns INVALID_EMAIL_OR_PASSWORD for wrong password`() =
        withTestAuthClient { client, _ ->
            client.register(username, email, password)

            val response = client.login(email, "WrongPassword")

            assertEquals(LoginResult.INVALID_EMAIL_OR_PASSWORD, response.result)
            assertTrue(response.accessToken.isEmpty())
        }
}
