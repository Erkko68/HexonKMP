package eric.bitria.hexon.auth.mock

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.email.smtp.SmtpService
import eric.bitria.hexon.account.password.ChangePasswordService
import eric.bitria.hexon.auth.password.PasswordService
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordResponse
import eric.bitria.hexon.dtos.account.ResetPasswordResult
import eric.bitria.hexon.utils.Validators.isValidPassword

class MockChangePasswordService(
    private val repository: AuthRepository,
    private val passwordService: PasswordService,
    private val smtpService: SmtpService
) : ChangePasswordService {

    override suspend fun changeWithOldPassword(userId: String, request: ChangePasswordRequest): ChangePasswordResponse {
        if (!isValidPassword(request.newPassword)) {
            return ChangePasswordResponse(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, "Weak password")
        }

        if (!repository.emailExists(request.email)) {
            return ChangePasswordResponse(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, "User not found")
        }

        if (request.resetCode != null) {
            val savedCode = repository.getUserCodeByUserId(request.email)
            if (savedCode == null || savedCode != request.resetCode) {
                return ChangePasswordResponse(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, "Invalid or expired code")
            }
            repository.clearUserCode(request.email)
        } else if (request.oldPassword != null) {
            if (!passwordService.verifyPassword(userId, request.oldPassword!!)) {
                return ChangePasswordResponse(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, "Invalid old password")
            }
        } else {
            return ChangePasswordResponse(ChangePasswordResult.INVALID_PASSWORD_OR_CODE, "Missing old password or reset code")
        }

        val newHash = BCrypt.withDefaults().hashToString(12, request.newPassword.toCharArray())
        repository.updatePasswordByUserId(request.email, newHash)

        return ChangePasswordResponse(ChangePasswordResult.SUCCESS, "Password changed successfully")
    }

    override suspend fun resetPassword(request: ResetPasswordRequest): ResetPasswordResponse {
        if (!repository.emailExists(request.email)) {
             return ResetPasswordResponse(ResetPasswordResult.SUCCESS, "If the email exists, a code was sent")
        }

        val resetCode = (100000..999999).random().toString()
        repository.updateUserCodeByEmail(request.email, resetCode)
        smtpService.sendEmail(request.email, "Reset Password", resetCode)

        return ResetPasswordResponse(ResetPasswordResult.SUCCESS, "If the email exists, a code was sent")
    }
}
