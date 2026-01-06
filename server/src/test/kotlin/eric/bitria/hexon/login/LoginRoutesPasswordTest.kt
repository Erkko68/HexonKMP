package eric.bitria.hexon.login

import eric.bitria.hexon.login
import eric.bitria.hexon.withTestAuthClient
import eric.bitria.hexon.dtos.auth.LoginResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LoginRoutesPasswordTest {

    private val email = "alice@test.com"

    @Test
    fun `login returns INVALID_EMAIL_OR_PASSWORD if password is too long`() = withTestAuthClient { client, _ ->
        val response = client.login(
            email,
            "AnExtremelyLongPasswordThatShouldBeRejected123"
        )
        assertEquals(LoginResult.INVALID_EMAIL_OR_PASSWORD, response.result)
    }

    @Test
    fun `login returns INVALID_EMAIL_OR_PASSWORD for weak password`() = withTestAuthClient { client, _ ->
        val response = client.login(email, "1234")
        assertEquals(LoginResult.INVALID_EMAIL_OR_PASSWORD, response.result)
    }

    @Test
    fun `login returns INVALID_EMAIL_OR_PASSWORD if password is empty`() = withTestAuthClient { client, _ ->
        val response = client.login(email, "")
        assertEquals(LoginResult.INVALID_EMAIL_OR_PASSWORD, response.result)
    }

    @Test
    fun `login rejects password with emoji`() = withTestAuthClient { client, _ ->
        val response = client.login(
            email,
            "EmojiPass123😊"
        )
        assertEquals(LoginResult.INVALID_EMAIL_OR_PASSWORD, response.result)
    }

    @Test
    fun `login rejects password exceeding max length`() = withTestAuthClient { client, _ ->
        val tooLongPassword = "Aa1_" + "x".repeat(29) // 33 chars
        val response = client.login(email, tooLongPassword)
        assertEquals(LoginResult.INVALID_EMAIL_OR_PASSWORD, response.result)
    }
}
