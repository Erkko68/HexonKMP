package eric.bitria.hexon.refresh

import eric.bitria.hexon.refresh
import eric.bitria.hexon.register
import eric.bitria.hexon.verify
import eric.bitria.hexon.withTestAuthClient
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RefreshRoutesTest {

    @Test
    fun `refresh flow after registration and verification`() = withTestAuthClient { client, inbox ->
        val email = "test@example.com"
        
        // 1. Register
        val regResponse = client.register("testuser", email, "Password123!")
        assertEquals(RegisterResult.VERIFICATION_SENT, regResponse.result)
        
        val code = inbox.value
        
        // 2. Verify Email to get initial tokens
        val verifyResponse = client.verify(email, code)
        assertEquals(VerifyEmailResult.SUCCESS, verifyResponse.result)
        val initialRefreshToken = verifyResponse.refreshToken
        
        // 3. Refresh tokens
        val refreshResponse: RefreshResponse = client.refresh(initialRefreshToken)
        
        assertEquals(RefreshResult.SUCCESS, refreshResponse.result)
        assertTrue(refreshResponse.accessToken.isNotEmpty(), "Access token should not be empty")
        assertTrue(refreshResponse.refreshToken.isNotEmpty(), "Refresh token should not be empty")
        
        // The mock service should now generate a different token because of the counter
        println("initialRefreshToken: $initialRefreshToken")
        println("refreshResponse.refreshToken: ${refreshResponse.refreshToken}")
        assertNotEquals(initialRefreshToken, refreshResponse.refreshToken, "Should receive a new refresh token")
    }

    @Test
    fun `refresh returns INVALID_TOKEN for malformed token`() = withTestAuthClient { client, _ ->
        val response: RefreshResponse = client.refresh("malformed-token-xyz")

        assertEquals(RefreshResult.INVALID_TOKEN, response.result)
        assertEquals("", response.accessToken)
        assertEquals("", response.refreshToken)
    }

    @Test
    fun `refresh returns INVALID_TOKEN for expired token`() = withTestAuthClient { client, _ ->
        // Using "expired-token" which is handled by MockTokenService to return null (invalid/expired)
        val response: RefreshResponse = client.refresh("expired-token")

        assertEquals(RefreshResult.INVALID_TOKEN, response.result)
        assertEquals("", response.accessToken)
        assertEquals("", response.refreshToken)
    }
}
