package eric.bitria.auth.verify

import eric.bitria.auth.register
import eric.bitria.auth.verify
import eric.bitria.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VerifyRoutesCodeTest {

    @Test
    fun `verify returns INVALID_VERIFICATION_CODE if code is empty`() =
        withTestAuthClient { client ->
            val body = client.verify(
                email = "alice@test.com",
                code = ""
            )

            assertEquals(
                VerifyEmailResult.INVALID_VERIFICATION_CODE,
                body.result
            )
        }

    @Test
    fun `verify returns INVALID_VERIFICATION_CODE if code is not 6 digits`() =
        withTestAuthClient { client ->
            val body = client.verify(
                email = "alice@test.com",
                code = "12345"
            )

            assertEquals(
                VerifyEmailResult.INVALID_VERIFICATION_CODE,
                body.result
            )
        }

    @Test
    fun `verify returns INVALID_VERIFICATION_CODE if code contains letters`() =
        withTestAuthClient { client ->
            val body = client.verify(
                email = "alice@test.com",
                code = "12a456"
            )

            assertEquals(
                VerifyEmailResult.INVALID_VERIFICATION_CODE,
                body.result
            )
        }

    @Test
    fun `verify returns INVALID_VERIFICATION_CODE if code contains special chars`() =
        withTestAuthClient { client ->
            val body = client.verify(
                email = "alice@test.com",
                code = "12#456"
            )

            assertEquals(
                VerifyEmailResult.INVALID_VERIFICATION_CODE,
                body.result
            )
        }

    @Test
    fun `verify returns INVALID_VERIFICATION_CODE if code contains emoji`() =
        withTestAuthClient { client ->
            val body = client.verify(
                email = "alice@test.com",
                code = "12😀456"
            )

            assertEquals(
                VerifyEmailResult.INVALID_VERIFICATION_CODE,
                body.result
            )
        }

    @Test
    fun `verify returns INVALID_VERIFICATION_CODE if code is well formed but wrong`() =
        withTestAuthClient { client ->
            // first register so a code exists
            client.register(
                username = "alice",
                email = "alice@test.com",
                password = "Secret123"
            )

            val body = client.verify(
                email = "alice@test.com",
                code = "999999" // valid format, wrong value
            )

            assertEquals(
                VerifyEmailResult.INVALID_VERIFICATION_CODE,
                body.result
            )
        }
}
