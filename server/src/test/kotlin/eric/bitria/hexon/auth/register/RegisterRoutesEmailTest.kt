package eric.bitria.hexon.auth.register

import eric.bitria.hexon.auth.register
import eric.bitria.hexon.auth.verify
import eric.bitria.hexon.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.RegisterResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RegisterRoutesEmailTest {

    @Test
    fun `register returns INVALID_EMAIL for malformed email`() =
        withTestAuthClient { client, inBox ->
            val body = client.register("alice", "not-an-email", "Secret123!")
            assertEquals(RegisterResult.INVALID_EMAIL, body.result)
        }

    @Test
    fun `register returns INVALID_EMAIL if email is too long`() =
        withTestAuthClient { client, inBox ->
            val body = client.register(
                "alice",
                "anEmailShouldNotBeThisLongSpeciallyForAStandardUser@AndNeitherShouldTheDomainBeThatLong.com",
                "Secret123!"
            )
            assertEquals(RegisterResult.INVALID_EMAIL, body.result)
        }

    @Test
    fun `register returns INVALID_EMAIL if email is empty`() = withTestAuthClient { client, inBox ->
        val body = client.register("alice", "", "Secret123!")
        assertEquals(RegisterResult.INVALID_EMAIL, body.result)
    }

    @Test
    fun `register returns EMAIL_EXISTS if email already verified`() =
        withTestAuthClient { client, inBox ->
            // 1. Register Alice
            client.register("alice", "alice@test.com", "Secret123!")

            // 2. Verify Alice
            val code = inBox.value
            client.verify("alice@test.com", code)

            // 3. Try to register another user with Alice's email
            val body = client.register("bob", "alice@test.com", "AnotherPass123!")
            assertEquals(RegisterResult.EMAIL_EXISTS, body.result)
        }

    @Test
    fun `register returns VERIFICATION_SENT if email exists but is NOT verified`() =
        withTestAuthClient { client, inBox ->
            // 1. Register Alice (don't verify)
            client.register("alice", "alice@test.com", "Secret123!")

            // 2. Register again with same email (should override and resend code)
            val body = client.register("alice2", "alice@test.com", "Secret123!")
            assertEquals(RegisterResult.VERIFICATION_SENT, body.result)
        }
}
