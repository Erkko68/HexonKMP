package eric.bitria.hexon.auth.mock

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.auth.email.EmailService
import eric.bitria.hexon.account.password.PasswordService
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordResponse
import eric.bitria.hexon.dtos.account.ForgotPasswordResult
import eric.bitria.hexon.utils.Validators.isValidPassword

class MockPasswordService(
    private val repository: AuthRepository,
    private val emailService: EmailService
) : PasswordService {

    override suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse {
        if (!isValidPassword(request.newPassword)) {
            return ChangePasswordResponse(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, "Weak password")
        }

        if (!repository.emailExists(request.email)) {
            return ChangePasswordResponse(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, "User not found")
        }

        if (request.resetCode != null) {
            val savedCode = repository.getUserCodeByEmail(request.email)
            if (savedCode == null || savedCode != request.resetCode) {
                return ChangePasswordResponse(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, "Invalid or expired code")
            }
            repository.clearUserCode(request.email)
        } else if (request.oldPassword != null) {
            val currentHash = repository.getPasswordByEmail(request.email) ?: return ChangePasswordResponse(ChangePasswordResult.UNKNOWN_ERROR, "User not found")
            val check = BCrypt.verifyer().verify(request.oldPassword!!.toCharArray(), currentHash)
            if (!check.verified) {
                return ChangePasswordResponse(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, "Invalid old password")
            }
        } else {
            return ChangePasswordResponse(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, "Missing old password or reset code")
        }

        val newHash = BCrypt.withDefaults().hashToString(12, request.newPassword.toCharArray())
        repository.updatePassword(request.email, newHash)

        return ChangePasswordResponse(ChangePasswordResult.SUCCESS, "Password changed successfully")
    }

    override suspend fun forgotPasswordCodeRequest(request: ForgotPasswordRequest): ForgotPasswordResponse {
        if (!repository.emailExists(request.email)) {
             return ForgotPasswordResponse(ForgotPasswordResult.SUCCESS, "If the email exists, a code was sent")
        }

        val resetCode = (100000..999999).random().toString()
        repository.updateUserCodeByEmail(request.email, resetCode)
        emailService.sendEmail(request.email, "Reset Password", resetCode)

        return ForgotPasswordResponse(ForgotPasswordResult.SUCCESS, "If the email exists, a code was sent")
    }
}
