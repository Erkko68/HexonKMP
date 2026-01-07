package eric.bitria.hexon.auth.password

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.auth.email.EmailService
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.auth.*
import eric.bitria.hexon.utils.Validators

class PasswordServiceImp(
    private val repository: AuthRepository,
    private val emailService: EmailService
) : PasswordService {

    override suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse {
        // Basic validation
        if (!Validators.isValidPassword(request.newPassword)) {
            return ChangePasswordResponse(
                result = ChangePasswordResult.INVALID_PASSWORD_OR_CODE,
                message = "Invalid new password format."
            )
        }

        // Case 1: Password reset via code (Forgot Password flow)
        if (request.resetCode != null) {
            val savedCode = repository.getResetCodeByEmail(request.email)
            if (savedCode == null || savedCode != request.resetCode) {
                return ChangePasswordResponse(
                    result = ChangePasswordResult.INVALID_PASSWORD_OR_CODE,
                    message = "Invalid or expired reset code."
                )
            }

            val hashedPassword = BCrypt.withDefaults().hashToString(12, request.newPassword.toCharArray())
            repository.updatePassword(request.email, hashedPassword)
            repository.clearResetCode(request.email)

            return ChangePasswordResponse(
                result = ChangePasswordResult.SUCCESS,
                message = "Password updated successfully."
            )
        }

        // Case 2: Password change via old password (Logged in flow)
        if (request.oldPassword != null) {
            val currentHash =
                repository.getPasswordByEmail(request.email) ?: return ChangePasswordResponse(
                    result = ChangePasswordResult.INVALID_PASSWORD_OR_CODE,
                    message = "User not found."
                )

            val passwordMatch = BCrypt.verifyer().verify(request.oldPassword!!.toCharArray(), currentHash).verified
            if (!passwordMatch) {
                return ChangePasswordResponse(
                    result = ChangePasswordResult.INVALID_PASSWORD_OR_CODE,
                    message = "Incorrect old password."
                )
            }

            val hashedPassword = BCrypt.withDefaults().hashToString(12, request.newPassword.toCharArray())
            repository.updatePassword(request.email, hashedPassword)

            return ChangePasswordResponse(
                result = ChangePasswordResult.SUCCESS,
                message = "Password updated successfully."
            )
        }

        return ChangePasswordResponse(
            result = ChangePasswordResult.INVALID_PASSWORD_OR_CODE,
            message = "Either reset code or old password must be provided."
        )
    }

    override suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse {
        if (!Validators.isValidEmail(request.email)) {
            return ForgotPasswordResponse(
                result = ForgotPasswordResult.UNKNOWN_ERROR,
                message = "Invalid email format."
            )
        }

        if (!repository.emailExists(request.email)) {
            // Security best practice: don't reveal if email exists, 
            // but for this app's logic we'll return a success-like message or a specific error.
            return ForgotPasswordResponse(
                result = ForgotPasswordResult.SUCCESS,
                message = "If an account exists with this email, a reset code has been sent."
            )
        }

        val resetCode = (100000..999999).random().toString()
        repository.updateResetCode(request.email, resetCode)

        emailService.sendEmail(
            to = request.email,
            subject = "Password Reset Code",
            body = "Your password reset code is: $resetCode"
        )

        return ForgotPasswordResponse(
            result = ForgotPasswordResult.SUCCESS,
            message = "If an account exists with this email, a reset code has been sent."
        )
    }
}