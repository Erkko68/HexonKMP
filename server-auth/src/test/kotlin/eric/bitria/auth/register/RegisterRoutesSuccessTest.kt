package eric.bitria.auth.register

import eric.bitria.auth.register
import eric.bitria.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.RegisterResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RegisterRoutesSuccessTest {

    @Test
    fun `register returns VERIFICATION_SENT for valid request`() = withTestAuthClient { client, inBox ->
        val body = client.register("alice", "alice@test.com", "Secret123!")
        assertEquals(RegisterResult.VERIFICATION_SENT, body.result)
    }
}