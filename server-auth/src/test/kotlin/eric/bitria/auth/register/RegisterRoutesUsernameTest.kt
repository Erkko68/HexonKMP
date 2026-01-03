package eric.bitria.auth.register

import eric.bitria.auth.register
import eric.bitria.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.RegisterResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RegisterRoutesUsernameTest {

    @Test
    fun `register returns INVALID_USERNAME if username is empty`() = withTestAuthClient { client, inBox ->
        val body = client.register("", "alice@test.com", "Secret123!")
        assertEquals(RegisterResult.INVALID_USERNAME, body.result)
    }

    @Test
    fun `register returns INVALID_USERNAME for malformed username`() = withTestAuthClient { client, inBox ->
        val body = client.register("@∞%ji?+--", "alice@test.com", "Secret123!")
        assertEquals(RegisterResult.INVALID_USERNAME, body.result)
    }

    @Test
    fun `register returns INVALID_USERNAME if username is too long`() = withTestAuthClient { client, inBox ->
        val body = client.register("AnUserNameShouldntBeThisLong", "alice@test.com", "Secret123!")
        assertEquals(RegisterResult.INVALID_USERNAME, body.result)
    }

    @Test
    fun `register returns USERNAME_EXISTS if username already used`() = withTestAuthClient { client, inBox ->
        // first registration should succeed
        val _ = client.register("alice", "alice@test.com", "Secret123!")

        // second registration with same username
        val body = client.register("alice", "alice2@test.com", "AnotherPass123!")
        assertEquals(RegisterResult.USERNAME_EXISTS, body.result)
    }

}