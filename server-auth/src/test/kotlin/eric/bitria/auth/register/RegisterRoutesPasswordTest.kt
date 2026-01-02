package eric.bitria.auth.register

import eric.bitria.auth.register
import eric.bitria.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.RegisterResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RegisterRoutesPasswordTest {

    @Test
    fun `register returns INVALID_PASSWORD for password too long`() = withTestAuthClient { client ->
        val body = client.register("alice", "alice@test.com", "AnExtremelyLongPasswordThatShouldBeRejected")
        assertEquals(RegisterResult.INVALID_PASSWORD, body.result)
    }

    @Test
    fun `register returns INVALID_PASSWORD for weak password`() = withTestAuthClient { client ->
        val body = client.register("alice", "alice@test.com", "1234")
        assertEquals(RegisterResult.INVALID_PASSWORD, body.result)
    }

    @Test
    fun `register returns INVALID_PASSWORD for empty password`() = withTestAuthClient { client ->
        val body = client.register("alice", "alice@test.com", "")
        assertEquals(RegisterResult.INVALID_PASSWORD, body.result)
    }
}