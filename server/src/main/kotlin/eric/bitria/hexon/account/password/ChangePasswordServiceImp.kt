package eric.bitria.hexon.account.password

import eric.bitria.hexon.auth.password.PasswordService
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordResponse
import eric.bitria.hexon.dtos.account.ResetPasswordResult
import eric.bitria.hexon.utils.Validators

class ChangePasswordServiceImp(
    private val authRepository: AuthRepository,
    private val passwordService: PasswordService,
) : ChangePasswordService {

    override suspend fun changeWithOldPassword(userId: String, request: ChangePasswordRequest): ChangePasswordResponse {
        if (!Validators.isValidPassword(request.newPassword)) {
            return ChangePasswordResponse(
                result = ChangePasswordResult.INVALID_PASSWORD_FORMAT,
                message = "Invalid new password format."
            )
        }

        if (!Validators.isValidPassword(request.oldPassword)) {
            return ChangePasswordResponse(
                result = ChangePasswordResult.INVALID_PASSWORD_FORMAT,
                message = "Invalid old password format."
            )
        }

        if (!passwordService.verifyPassword(userId, request.oldPassword)) {
            return ChangePasswordResponse(
                result = ChangePasswordResult.INVALID_PASSWORD,
                message = "Incorrect old password."
            )
        }

        passwordService.updatePassword(userId, request.newPassword)
        return ChangePasswordResponse(
            result = ChangePasswordResult.SUCCESS,
            message = "Password updated successfully."
        )
    }

    override suspend fun resetPassword(request: ResetPasswordRequest): ResetPasswordResponse {
        if (!Validators.isValidCode(request.code)) {
            return ResetPasswordResponse(
                result = ResetPasswordResult.INVALID_CODE_FORMAT,
                message = "Invalid code format."
            )
        }

        if (!Validators.isValidPassword(request.newPassword)) {
            return ResetPasswordResponse(
                result = ResetPasswordResult.INVALID_PASSWORD_FORMAT,
                message = "Invalid new password format."
            )
        }

        val userId = authRepository.getUserIdByEmail(request.email) ?: return ResetPasswordResponse(
            result = ResetPasswordResult.UNKNOWN_EMAIL,
            message = "Invalid code."
        )

        val storedCode = authRepository.getUserCodeByUserId(userId)
        if (storedCode != request.code) {
            return ResetPasswordResponse(
                result = ResetPasswordResult.INVALID_CODE,
                message = "Invalid code."
            )
        }

        passwordService.updatePassword(userId, request.newPassword)
        authRepository.clearUserCode(request.email)

        return ResetPasswordResponse(
            result = ResetPasswordResult.SUCCESS,
            message = "Password reset successfully."
        )
    }
}