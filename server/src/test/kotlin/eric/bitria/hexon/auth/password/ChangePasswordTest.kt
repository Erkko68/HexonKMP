package eric.bitria.hexon.auth.password

import eric.bitria.hexon.auth.changePassword
import eric.bitria.hexon.auth.login
import eric.bitria.hexon.auth.register
import eric.bitria.hexon.auth.verify
import eric.bitria.hexon.auth.withTestAuthClient
import eric.bitria.hexon.dtos.auth.ChangePasswordResult
import eric.bitria.hexon.dtos.auth.LoginResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ChangePasswordTest {

    @Test
    fun `test change password with old password success`() = withTestAuthClient { client, inbox ->
        val email = "change@example.com"
        val oldPassword = "OldPassword123!"
        val newPassword = "NewPassword123!"

        client.register("changeuser", email, oldPassword)
        client.verify(email, inbox.value)

        val changeResp = client.changePassword(
            email = email,
            oldPassword = oldPassword,
            newPassword = newPassword
        )
        assertEquals(ChangePasswordResult.SUCCESS, changeResp.result)

        val loginResp = client.login(email, newPassword)
        assertEquals(LoginResult.SUCCESS, loginResp.result)
    }

    @Test
    fun `test change password with wrong old password fails`() = withTestAuthClient { client, inbox ->
        val email = "wrongold@example.com"
        val actualPassword = "ActualPassword123!"

        client.register("wrongold", email, actualPassword)
        client.verify(email, inbox.value)

        val changeResp = client.changePassword(
            email = email,
            oldPassword = "WrongPassword123!",
            newPassword = "NewPassword123!"
        )
        assertEquals(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, changeResp.result)
    }

    @Test
    fun `test change password with weak password fails`() = withTestAuthClient { client, inbox ->
        val email = "weak@example.com"
        val oldPassword = "Password123!"

        client.register("weakuser", email, oldPassword)
        client.verify(email, inbox.value)

    val changeResp = client.changePassword(
            email = email,
            oldPassword = oldPassword,
            newPassword = "123" // Weak
        )
        assertEquals(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, changeResp.result)
    }

    @Test
    fun `test change password for non-existent user fails`() = withTestAuthClient { client, _ ->
        val changeResp = client.changePassword(
            email = "nonexistent@example.com",
            oldPassword = "AnyPassword123!",
            newPassword = "NewPassword123!"
        )
        assertEquals(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, changeResp.result)
    }
}
