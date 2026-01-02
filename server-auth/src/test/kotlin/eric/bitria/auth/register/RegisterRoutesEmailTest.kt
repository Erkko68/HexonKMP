package eric.bitria.auth.register

import eric.bitria.auth.register
import eric.bitria.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.RegisterResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RegisterRoutesEmailTest {

    @Test
    fun `register returns INVALID_EMAIL for malformed email`() = withTestAuthClient { client ->
        val body = client.register("alice", "not-an-email", "Secret123!")
        assertEquals(RegisterResult.INVALID_EMAIL, body.result)
    }

    @Test
    fun `register returns INVALID_EMAIL for too long email`() = withTestAuthClient { client ->
        val body = client.register("alice", "anEmailShouldNotBeThisLong@AndNeitherShouldTheDomain.com", "Secret123!")
        assertEquals(RegisterResult.INVALID_EMAIL, body.result)
    }

    @Test
    fun `register returns INVALID_EMAIL for an empty email`() = withTestAuthClient { client ->
        val body = client.register("alice", "@", "Secret123!")
        assertEquals(RegisterResult.INVALID_EMAIL, body.result)
    }

    @Test
    fun `register returns EMAIL_EXISTS if email already used`() = withTestAuthClient { client ->
        // first registration should succeed
        val _ = client.register("alice", "alice@test.com", "Secret123!")

        // second registration with same email
        val body = client.register("bob", "alice@test.com", "AnotherPass123!")
        assertEquals(RegisterResult.EMAIL_EXISTS, body.result)
    }
}