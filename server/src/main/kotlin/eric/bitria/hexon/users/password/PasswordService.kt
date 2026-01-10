package eric.bitria.hexon.users.password

import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordResponse
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordResponse

interface PasswordService {

    /**
     * Changes the password for an authenticated user.
     * Requires the current (old) password.
     */
    suspend fun changePassword(
        userId: String,
        request: ChangePasswordRequest
    ): ChangePasswordResponse

    /**
     * Starts the forgot password flow.
     * Sends a 6-digit verification code to the user's email.
     */
    suspend fun forgotPassword(
        request: ForgotPasswordRequest
    ): ForgotPasswordResponse

    /**
     * Resets the user's password using a 6-digit email verification code.
     */
    suspend fun resetPassword(
        request: ResetPasswordRequest
    ): ResetPasswordResponse
}