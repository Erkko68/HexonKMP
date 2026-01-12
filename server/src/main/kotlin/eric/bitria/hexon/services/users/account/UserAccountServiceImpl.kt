package eric.bitria.hexon.services.users.account

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.dtos.account.ConfirmDeleteAccountRequest
import eric.bitria.hexon.dtos.account.ConfirmDeleteAccountResponse
import eric.bitria.hexon.dtos.account.DeleteAccountResult
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordResponse
import eric.bitria.hexon.dtos.account.ForgotPasswordResult
import eric.bitria.hexon.dtos.account.RequestDeleteAccountResponse
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordResponse
import eric.bitria.hexon.dtos.account.ResetPasswordResult
import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.services.email.verification.EmailVerificationService
import eric.bitria.hexon.utils.Validators

class UserAccountServiceImpl(
    private val authRepository: AuthRepository,
    private val emailVerificationService: EmailVerificationService,
) : UserAccountService {

    override suspend fun changePassword(userId: String, request: ChangePasswordRequest): ChangePasswordResponse {
        if (!Validators.isValidPassword(request.newPassword)) {
            return ChangePasswordResponse(
                ChangePasswordResult.INVALID_PASSWORD,
                "New password does not meet security requirements."
            )
        }

        // 1. Fetch User (to get current password hash)
        val user = authRepository.findUserById(userId)
            ?: return ChangePasswordResponse(ChangePasswordResult.USER_NOT_FOUND, "User not found")

        // 2. Verify OLD Password
        val oldPasswordMatches = BCrypt.verifyer().verify(
            request.oldPassword.toCharArray(),
            user.password
        ).verified

        if (!oldPasswordMatches) {
            return ChangePasswordResponse(
                ChangePasswordResult.WRONG_PASSWORD,
                "The old password you entered is incorrect."
            )
        }


        // 3. Hash NEW Password
        val newHash = BCrypt.withDefaults().hashToString(12, request.newPassword.toCharArray())

        // 4. Update DB
        authRepository.updatePassword(userId, newHash)

        // Invalidate old refresh tokens
        authRepository.updateRefreshToken(userId, null)

        return ChangePasswordResponse(
            ChangePasswordResult.SUCCESS,
            "Password changed successfully."
        )
    }

    override suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse {
        // 0. Check if email is valid
        if(!Validators.isValidEmail(request.email)) {
            return ForgotPasswordResponse(
                ForgotPasswordResult.INVALID_EMAIL,
                "Invalid email format."
            )
        }
        // 1. Check if user exists
        authRepository.findUserByEmail(request.email)
            ?: return ForgotPasswordResponse(
                ForgotPasswordResult.SUCCESS,
                "A password reset code has been sent to your email."
            )

        // 2. Send Verification Code (Type: PASSWORD_RESET)
        emailVerificationService.sendVerificationCodeByEmail(
            email = request.email,
            type = EmailVerificationType.PASSWORD_RESET
        )

        return ForgotPasswordResponse(
            ForgotPasswordResult.SUCCESS,
            "A password reset code has been sent to your email."
        )
    }

    override suspend fun resetPassword(request: ResetPasswordRequest): ResetPasswordResponse {
        // 0. Check if email is valid
        if(!Validators.isValidEmail(request.email)) {
            return ResetPasswordResponse(
                ResetPasswordResult.INVALID_EMAIL,
                "Invalid email format."
            )
        }

        // 1. Check Password Strength
        if (!Validators.isValidPassword(request.newPassword)) {
            return ResetPasswordResponse(
                ResetPasswordResult.INVALID_PASSWORD,
                "Password must be at least 8 characters..."
            )
        }

        // 2. Verify the Code
        if(!Validators.isValidCode(request.code)) {
            return ResetPasswordResponse(
                ResetPasswordResult.INVALID_CODE,
                "Invalid or expired reset code."
            )
        }
        // This checks the DB for a code matching: Email + Type(PASSWORD_RESET) + Hash + NotExpired
        val isCodeValid = emailVerificationService.verifyCodeByEmail(
            email = request.email,
            code = request.code,
            type = EmailVerificationType.PASSWORD_RESET
        )

        if (!isCodeValid) {
            return ResetPasswordResponse(
                ResetPasswordResult.INVALID_CODE,
                "Invalid or expired reset code."
            )
        }

        // 3. Find User & Update Password
        val user = authRepository.findUserByEmail(request.email)
            ?: return ResetPasswordResponse(ResetPasswordResult.USER_NOT_FOUND, "User not found")

        val newHash = BCrypt.withDefaults().hashToString(12, request.newPassword.toCharArray())

        authRepository.updatePassword(user.id, newHash)

        // 4. Security: Revoke all existing sessions (Invalidate Refresh Tokens)
        // This forces the user to log in again on all devices with the new password.
        authRepository.updateRefreshToken(user.id, null)

        return ResetPasswordResponse(
            ResetPasswordResult.SUCCESS,
            "Your password has been reset successfully. Please log in."
        )
    }

    override suspend fun requestAccountDeletion(userId: String): RequestDeleteAccountResponse {
        // 1. Get User Email (We need it to send the code)
        val user = authRepository.findUserById(userId)
            ?: throw IllegalStateException("User does not exist.")

        // 2. Send Verification Code (Type: DELETE_ACCOUNT)
        emailVerificationService.sendVerificationCodeByEmail(
            email = user.email,
            type = EmailVerificationType.ACCOUNT_DELETION
        )

        return RequestDeleteAccountResponse(
            "A verification code has been sent to your email (${user.email})."
        )
    }

    override suspend fun confirmAccountDeletion(
        userId: String,
        request: ConfirmDeleteAccountRequest
    ): ConfirmDeleteAccountResponse {

        // 1. Fetch User
        val user = authRepository.findUserById(userId)
            ?: return ConfirmDeleteAccountResponse(DeleteAccountResult.USER_NOT_FOUND, "User not found")

        // 2. Verify Password (First Layer of Defense)
        val isPasswordCorrect = BCrypt.verifyer().verify(
            request.password.toCharArray(),
            user.password
        ).verified

        if (!isPasswordCorrect) {
            return ConfirmDeleteAccountResponse(
                DeleteAccountResult.WRONG_PASSWORD,
                "Incorrect password."
            )
        }

        // 3. Verify Email Code (Second Layer of Defense)
        val isCodeValid = emailVerificationService.verifyCodeByEmail(
            email = user.email,
            code = request.code,
            type = EmailVerificationType.ACCOUNT_DELETION
        )

        if (!isCodeValid) {
            return ConfirmDeleteAccountResponse(
                DeleteAccountResult.INVALID_CODE,
                "Invalid or expired verification code."
            )
        }

        // 4. Delete Account
        authRepository.deleteUser(userId)

        return ConfirmDeleteAccountResponse(
            DeleteAccountResult.SUCCESS,
            "Your account has been permanently deleted."
        )
    }
}