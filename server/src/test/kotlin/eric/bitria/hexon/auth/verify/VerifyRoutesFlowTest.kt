package eric.bitria.hexon.auth.verify

import eric.bitria.hexon.auth.register
import eric.bitria.hexon.auth.resendVerification
import eric.bitria.hexon.auth.verify
import eric.bitria.hexon.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VerifyRoutesFlowTest {

    private val email = "alice@test.com"
    private val password = "Secret123"

    @Test
    fun `verify returns SUCCESS for correct code`() =
        withTestAuthClient { client, inBox ->
            client.register("alice", email, password)

            val body = client.verify(
                email = email,
                code = inBox.value
            )

            assertEquals(
                VerifyEmailResult.SUCCESS,
                body.result
            )
        }

    @Test
    fun `verify returns ACCOUNT_ALREADY_VERIFIED when verifying twice`() =
        withTestAuthClient { client, inBox ->
            client.register("alice", email, password)

            // first verification
            client.verify(
                email = email,
                code = inBox.value
            )

            // second verification attempt
            val body = client.verify(
                email = email,
                code = inBox.value
            )

            assertEquals(
                VerifyEmailResult.ACCOUNT_ALREADY_VERIFIED,
                body.result
            )
        }

    @Test
    fun `verify returns ACCOUNT_ALREADY_VERIFIED even with wrong code after verification`() =
        withTestAuthClient { client, inBox ->
            client.register("alice", email, password)

            client.verify(
                email = email,
                code = inBox.value
            )

            val body = client.verify(
                email = email,
                code = inBox.value
            )

            assertEquals(
                VerifyEmailResult.ACCOUNT_ALREADY_VERIFIED,
                body.result
            )
        }

    @Test
    fun `verify succeeds after resending verification code`() =
        withTestAuthClient { client, inBox ->
            client.register("alice", email, password)

            // resend code
            client.resendVerification(email)

            val body = client.verify(
                email = email,
                code = inBox.value
            )

            assertEquals(
                VerifyEmailResult.SUCCESS,
                body.result
            )
        }

    @Test
    fun `verify returns INVALID_VERIFICATION_CODE when using old code after resending verification code`() =
        withTestAuthClient { client, inBox ->
            client.register("alice", email, password)

            val oldCode = inBox.value

            // resend code
            client.resendVerification(email)


            val body = client.verify(
                email = email,
                code = oldCode
            )

            assertEquals(
                VerifyEmailResult.INVALID_VERIFICATION_CODE,
                body.result
            )
        }
}
