package eric.bitria.auth.verify

import eric.bitria.auth.register
import eric.bitria.auth.resendVerification
import eric.bitria.auth.verify
import eric.bitria.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VerifyRoutesFlowTest {

    private val email = "alice@test.com"
    private val password = "Secret123"
    private val correctCode = "123456"

    @Test
    fun `verify returns SUCCESS for correct code`() =
        withTestAuthClient { client ->
            client.register("alice", email, password)

            val body = client.verify(
                email = email,
                code = correctCode
            )

            assertEquals(
                VerifyEmailResult.SUCCESS,
                body.result
            )
        }

    @Test
    fun `verify returns ACCOUNT_ALREADY_VERIFIED when verifying twice`() =
        withTestAuthClient { client ->
            client.register("alice", email, password)

            // first verification
            client.verify(
                email = email,
                code = correctCode
            )

            // second verification attempt
            val body = client.verify(
                email = email,
                code = correctCode
            )

            assertEquals(
                VerifyEmailResult.ACCOUNT_ALREADY_VERIFIED,
                body.result
            )
        }

    @Test
    fun `verify returns ACCOUNT_ALREADY_VERIFIED even with wrong code after verification`() =
        withTestAuthClient { client ->
            client.register("alice", email, password)

            client.verify(
                email = email,
                code = correctCode
            )

            val body = client.verify(
                email = email,
                code = "999999"
            )

            assertEquals(
                VerifyEmailResult.ACCOUNT_ALREADY_VERIFIED,
                body.result
            )
        }

    @Test
    fun `verify succeeds after resending verification code`() =
        withTestAuthClient { client ->
            client.register("alice", email, password)

            // resend code
            client.resendVerification(email)

            val body = client.verify(
                email = email,
                code = correctCode
            )

            assertEquals(
                VerifyEmailResult.SUCCESS,
                body.result
            )
        }
}
