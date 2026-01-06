package eric.bitria.hexon.login

import eric.bitria.hexon.login
import eric.bitria.hexon.register
import eric.bitria.hexon.verify
import eric.bitria.hexon.withTestAuthClient
import eric.bitria.hexon.dtos.auth.LoginResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LoginRoutesTest {

    private val email = "test@example.com"
    private val password = "Password123!"
    private val username = "testuser"

    @Test
    fun `login returns SUCCESS for verified user with correct password`() =
        withTestAuthClient { client, inbox ->
            client.register(username, email, password)
            
            val code = inbox.value
            client.verify(email, code)

            val response = client.login(email, password)

            assertEquals(LoginResult.SUCCESS, response.result)
            assertNotNull(response.accessToken)
            assertTrue(response.accessToken.isNotEmpty())
            assertNotNull(response.refreshToken)
            assertTrue(response.refreshToken.isNotEmpty())
        }

    @Test
    fun `login returns PENDING_VERIFICATION for unverified user and resends code`() =
        withTestAuthClient { client, inbox ->
            client.register(username, email, password)
            val firstCode = inbox.value

            // Try to login without verifying
            val response = client.login(email, password)

            assertEquals(LoginResult.PENDING_VERIFICATION, response.result)
            assertTrue(response.accessToken.isEmpty())
            
            // Check if a NEW code was sent
            val secondCode = inbox.value
            assertTrue(firstCode != secondCode, "A new verification code should have been sent")
            
            // Verify with the second code should work
            val verifyResponse = client.verify(email, secondCode)
            assertEquals(eric.bitria.hexon.dtos.auth.VerifyEmailResult.SUCCESS, verifyResponse.result)
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

            val response = client.login(email, "WrongPassword123!")

            assertEquals(LoginResult.INVALID_EMAIL_OR_PASSWORD, response.result)
            assertTrue(response.accessToken.isEmpty())
        }
}
