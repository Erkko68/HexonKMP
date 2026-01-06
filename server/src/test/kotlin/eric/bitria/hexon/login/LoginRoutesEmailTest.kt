package eric.bitria.hexon.login

import eric.bitria.hexon.login
import eric.bitria.hexon.withTestAuthClient
import eric.bitria.hexon.dtos.auth.LoginResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LoginRoutesEmailTest {

    @Test
    fun `login returns INVALID_EMAIL_OR_PASSWORD for malformed email`() = withTestAuthClient { client, _ ->
        val response = client.login("not-an-email", "Secret123!")
        assertEquals(LoginResult.INVALID_EMAIL_OR_PASSWORD, response.result)
    }

    @Test
    fun `login returns INVALID_EMAIL_OR_PASSWORD if email is too long`() = withTestAuthClient { client, _ ->
        val response = client.login(
            "anEmailShouldNotBeThisLongSpeciallyForAStandardUser@AndNeitherShouldTheDomainBeThatLong.com",
            "Secret123!"
        )
        assertEquals(LoginResult.INVALID_EMAIL_OR_PASSWORD, response.result)
    }

    @Test
    fun `login returns INVALID_EMAIL_OR_PASSWORD if email is empty`() = withTestAuthClient { client, _ ->
        val response = client.login("", "Secret123!")
        assertEquals(LoginResult.INVALID_EMAIL_OR_PASSWORD, response.result)
    }
}
