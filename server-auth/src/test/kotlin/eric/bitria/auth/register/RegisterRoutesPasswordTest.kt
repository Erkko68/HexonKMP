package eric.bitria.auth.register

import eric.bitria.auth.register
import eric.bitria.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.RegisterResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RegisterRoutesPasswordTest {

    @Test
    fun `register returns INVALID_PASSWORD if password is too long`() = withTestAuthClient { client ->
        val body = client.register(
            "alice",
            "alice@test.com",
            "AnExtremelyLongPasswordThatShouldBeRejected123"
        )
        assertEquals(RegisterResult.INVALID_PASSWORD, body.result)
    }

    @Test
    fun `register returns INVALID_PASSWORD for weak password`() = withTestAuthClient { client ->
        val body = client.register("alice", "alice@test.com", "1234")
        assertEquals(RegisterResult.INVALID_PASSWORD, body.result)
    }

    @Test
    fun `register returns INVALID_PASSWORD if password is empty`() = withTestAuthClient { client ->
        val body = client.register("alice", "alice@test.com", "")
        assertEquals(RegisterResult.INVALID_PASSWORD, body.result)
    }

    @Test
    fun `register accepts password with allowed special characters`() = withTestAuthClient { client ->
        val body = client.register(
            "alice",
            "alice@test.com",
            "ValidPass123!@#"
        )
        assertEquals(RegisterResult.VERIFICATION_SENT, body.result)
    }

    @Test
    fun `register rejects password with emoji`() = withTestAuthClient { client ->
        val body = client.register(
            "alice",
            "alice@test.com",
            "EmojiPass123😊"
        )
        assertEquals(RegisterResult.INVALID_PASSWORD, body.result)
    }

    @Test
    fun `register accepts password at max length`() = withTestAuthClient { client ->
        val maxLengthPassword = "Aa1_" + "x".repeat(28) // 32 chars total
        val body = client.register("alice", "alice@test.com", maxLengthPassword)
        assertEquals(RegisterResult.VERIFICATION_SENT, body.result)
    }

    @Test
    fun `register rejects password exceeding max length`() = withTestAuthClient { client ->
        val tooLongPassword = "Aa1_" + "x".repeat(29) // 33 chars
        val body = client.register("alice", "alice@test.com", tooLongPassword)
        assertEquals(RegisterResult.INVALID_PASSWORD, body.result)
    }
}
