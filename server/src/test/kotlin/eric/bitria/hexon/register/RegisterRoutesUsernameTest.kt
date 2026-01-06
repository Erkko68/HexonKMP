package eric.bitria.hexon.register

import eric.bitria.hexon.register
import eric.bitria.hexon.withTestAuthClient
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
    fun `register returns USERNAME_EXISTS if username is taken by another unverified user`() = withTestAuthClient { client, inBox ->
        // Alice registers but doesn't verify
        client.register("alice", "alice@test.com", "Secret123!")

        // Bob tries to take 'alice' username
        val body = client.register("alice", "bob@test.com", "Secret123!")
        assertEquals(RegisterResult.USERNAME_EXISTS, body.result)
    }
}
