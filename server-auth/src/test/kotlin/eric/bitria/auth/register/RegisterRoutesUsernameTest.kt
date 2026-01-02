package eric.bitria.auth.register

import eric.bitria.auth.register
import eric.bitria.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.RegisterResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RegisterRoutesUsernameTest {

    @Test
    fun `register returns USERNAME_EXISTS if username already used`() = withTestAuthClient { client ->
        // first registration should succeed
        val _ = client.register("alice", "alice@test.com", "Secret123!")

        // second registration with same username
        val body = client.register("alice", "alice2@test.com", "AnotherPass123!")
        assertEquals(RegisterResult.USERNAME_EXISTS, body.result)
    }

}