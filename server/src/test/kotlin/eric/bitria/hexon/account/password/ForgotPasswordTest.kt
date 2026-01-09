package eric.bitria.hexon.account.password

import eric.bitria.hexon.account.changePassword
import eric.bitria.hexon.account.forgotPassword
import eric.bitria.hexon.account.withTestAccountClient
import eric.bitria.hexon.auth.login
import eric.bitria.hexon.auth.register
import eric.bitria.hexon.auth.verify
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.dtos.account.ResetPasswordResult
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ForgotPasswordTest {

    @Test
    fun `test forgot password flow success`() = withTestAccountClient { client, inbox ->
        val email = "test@example.com"
        val initialPassword = "Password123!"
        val newPassword = "NewPassword123!"

        // 1. Register and verify user
        val regResp = client.register("testuser", email, initialPassword)
        assertEquals(RegisterResult.VERIFICATION_SENT, regResp.result)
        val verifyResp = client.verify(email, inbox.value)
        assertEquals(VerifyEmailResult.SUCCESS, verifyResp.result)

        // 2. Request forgot password
        val forgotResp = client.forgotPassword(email)
        assertEquals(ResetPasswordResult.SUCCESS, forgotResp.result)
        val resetCode = inbox.value

        // 3. Change password using reset code
        val changeResp = client.changePassword(
            email = email,
            resetCode = resetCode,
            newPassword = newPassword
        )
        assertEquals(ChangePasswordResult.SUCCESS, changeResp.result)

        // 4. Verify old password fails and new password works
        val oldLogin = client.login(email, initialPassword)
        assertEquals(LoginResult.INVALID_EMAIL_OR_PASSWORD, oldLogin.result)

        val newLogin = client.login(email, newPassword)
        assertEquals(LoginResult.SUCCESS, newLogin.result)
    }

    @Test
    fun `test forgot password with wrong code fails`() = withTestAccountClient { client, inbox ->
        val email = "wrongcode@example.com"
        client.register("wrongcode", email, "Password123!")
        client.verify(email, inbox.value)

        client.forgotPassword(email)
        
        val changeResp = client.changePassword(
            email = email,
            resetCode = "000000",
            newPassword = "NewPassword123!"
        )
        assertEquals(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, changeResp.result)
    }

    @Test
    fun `test forgot password for non-existent user still returns success`() = withTestAccountClient { client, _ ->
        // For security, many APIs return success even if the email doesn't exist
        val forgotResp = client.forgotPassword("nonexistent@example.com")
        assertEquals(ResetPasswordResult.SUCCESS, forgotResp.result)
    }
}
