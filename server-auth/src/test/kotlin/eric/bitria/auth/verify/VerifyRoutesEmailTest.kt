package eric.bitria.auth.verify

import eric.bitria.auth.verify
import eric.bitria.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VerifyRoutesEmailTest {

    @Test
    fun `verify returns INVALID_EMAIL if email is empty`() = withTestAuthClient { client ->
        val body = client.verify(
            email = "",
            code = "123456"
        )

        assertEquals(VerifyEmailResult.INVALID_EMAIL, body.result)
    }

    @Test
    fun `verify returns INVALID_EMAIL if email is blank`() = withTestAuthClient { client ->
        val body = client.verify(
            email = "   ",
            code = "123456"
        )

        assertEquals(VerifyEmailResult.INVALID_EMAIL, body.result)
    }

    @Test
    fun `verify returns INVALID_EMAIL for malformed email`() = withTestAuthClient { client ->
        val body = client.verify(
            email = "not-an-email",
            code = "123456"
        )

        assertEquals(VerifyEmailResult.INVALID_EMAIL, body.result)
    }

    @Test
    fun `verify returns INVALID_EMAIL if email has no at sign`() = withTestAuthClient { client ->
        val body = client.verify(
            email = "alice.test.com",
            code = "123456"
        )

        assertEquals(VerifyEmailResult.INVALID_EMAIL, body.result)
    }

    @Test
    fun `verify returns INVALID_EMAIL if email has no domain`() = withTestAuthClient { client ->
        val body = client.verify(
            email = "alice@",
            code = "123456"
        )

        assertEquals(VerifyEmailResult.INVALID_EMAIL, body.result)
    }

    @Test
    fun `verify returns INVALID_EMAIL if email contains emoji`() = withTestAuthClient { client ->
        val body = client.verify(
            email = "alice😀@test.com",
            code = "123456"
        )

        assertEquals(VerifyEmailResult.INVALID_EMAIL, body.result)
    }

    @Test
    fun `verify returns INVALID_EMAIL if email exceeds max length`() = withTestAuthClient { client ->
        val longEmail =
            "a".repeat(40) + "@" + "b".repeat(40) + ".com"

        val body = client.verify(
            email = longEmail,
            code = "123456"
        )

        assertEquals(VerifyEmailResult.INVALID_EMAIL, body.result)
    }
}
