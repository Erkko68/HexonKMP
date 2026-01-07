package eric.bitria.hexon.auth.register

import eric.bitria.hexon.auth.register
import eric.bitria.hexon.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.RegisterResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RegisterRoutesSuccessTest {

    @Test
    fun `register returns VERIFICATION_SENT for valid request`() =
        withTestAuthClient { client, inBox ->
            val body = client.register("alice", "alice@test.com", "Secret123!")
            assertEquals(RegisterResult.VERIFICATION_SENT, body.result)
        }

    @Test
    fun `unverified user can register again with same email and same username`() =
        withTestAuthClient { client, inBox ->
            // First registration
            client.register("alice", "alice@test.com", "Secret123!")

            // Second registration with same details (should be allowed)
            val body = client.register("alice", "alice@test.com", "NewSecret123!")
            assertEquals(RegisterResult.VERIFICATION_SENT, body.result)
        }

    @Test
    fun `unverified user can register again with same email and different username`() =
        withTestAuthClient { client, inBox ->
            // First registration
            client.register("alice", "alice@test.com", "Secret123!")

            // Second registration with same email but different username (should be allowed)
            val body = client.register("alice_new", "alice@test.com", "Secret123!")
            assertEquals(RegisterResult.VERIFICATION_SENT, body.result)
        }

    @Test
    fun `unverified user can register again with same email and different password`() =
        withTestAuthClient { client, inBox ->
            // First registration
            client.register("alice", "alice@test.com", "Secret123!")

            // Second registration with same email but different password (should be allowed)
            val body = client.register("alice", "alice@test.com", "DifferentPass123!")
            assertEquals(RegisterResult.VERIFICATION_SENT, body.result)
        }
}
