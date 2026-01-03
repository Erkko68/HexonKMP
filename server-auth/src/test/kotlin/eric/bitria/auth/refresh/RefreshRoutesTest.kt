package eric.bitria.auth.refresh

import eric.bitria.auth.refresh
import eric.bitria.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefreshRoutesTest {

    @Test
    fun `refresh returns new tokens for valid refresh token`() = withTestAuthClient { client, inBox ->
        val request = "valid-refresh-token"
        val response: RefreshResponse = client.refresh(request)

        assertEquals(RefreshResult.SUCCESS, response.result)
        assertTrue(response.accessToken.isNotEmpty(), "Access token should not be empty")
        assertTrue(response.refreshToken.isNotEmpty(), "Refresh token should not be empty")
    }

    @Test
    fun `refresh returns INVALID_REFRESH_TOKEN for unknown token`() = withTestAuthClient { client, inBox ->
        val request = "unknown-token"
        val response: RefreshResponse = client.refresh(request)

        assertEquals(RefreshResult.INVALID_TOKEN, response.result)
        assertEquals("", response.accessToken)
        assertEquals("", response.refreshToken)
    }

    @Test
    fun `refresh returns EXPIRED_REFRESH_TOKEN for expired token`() = withTestAuthClient { client, inBox ->
        val request = "expired-token"
        val response: RefreshResponse = client.refresh(request)

        assertEquals(RefreshResult.EXPIRED_TOKEN, response.result)
        assertEquals("", response.accessToken)
        assertEquals("", response.refreshToken)
    }

    @Test
    fun `refresh returns UNKNOWN_ERROR for malformed request`() = withTestAuthClient { client, inBox ->
        val request = ""
        val response: RefreshResponse = client.refresh(request)

        assertEquals(RefreshResult.UNKNOWN_ERROR, response.result)
        assertEquals("", response.accessToken)
        assertEquals("", response.refreshToken)
    }
}
