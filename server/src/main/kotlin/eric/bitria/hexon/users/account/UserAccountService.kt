package eric.bitria.hexon.users.account

import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ConfirmDeleteAccountRequest
import eric.bitria.hexon.dtos.account.ConfirmDeleteAccountResponse
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordResponse
import eric.bitria.hexon.dtos.account.RequestDeleteAccountResponse
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordResponse

interface UserAccountService {

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

    /**
     * Requests the deletion of an account.
     * Sends a 6-digit verification code to the user's email.
     */
    suspend fun requestAccountDeletion(
        userId: String
    ): RequestDeleteAccountResponse


    /**
     * Confirms the deletion of an account.
     * Requires the user's password and a 6-digit verification code.
     */
    suspend fun confirmAccountDeletion(
        userId: String,
        request: ConfirmDeleteAccountRequest
    ): ConfirmDeleteAccountResponse
}