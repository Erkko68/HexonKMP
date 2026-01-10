package eric.bitria.hexon.users.mock

import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordResponse
import eric.bitria.hexon.dtos.account.ForgotPasswordResult
import eric.bitria.hexon.dtos.account.ResetPasswordRequest
import eric.bitria.hexon.dtos.account.ResetPasswordResponse
import eric.bitria.hexon.dtos.account.ResetPasswordResult
import eric.bitria.hexon.dtos.auth.EmailVerificationType
import eric.bitria.hexon.email.verification.EmailVerificationService
import eric.bitria.hexon.users.password.PasswordService

class MockPasswordService(
    private val authRepository: AuthRepository,
    private val emailVerificationService: EmailVerificationService
) : PasswordService {

    override suspend fun changePassword(userId: String, request: ChangePasswordRequest): ChangePasswordResponse {
        val user = authRepository.findUserById(userId)
            ?: return ChangePasswordResponse(ChangePasswordResult.USER_NOT_FOUND, "Not found")

        if (user.password != request.oldPassword) {
            return ChangePasswordResponse(ChangePasswordResult.WRONG_PASSWORD, "Wrong current password")
        }

        if (request.newPassword == "weak") {
            return ChangePasswordResponse(ChangePasswordResult.INVALID_PASSWORD, "Weak password")
        }

        authRepository.updatePassword(userId, request.newPassword)
        authRepository.updateRefreshToken(userId, null) // Revoke sessions

        return ChangePasswordResponse(ChangePasswordResult.SUCCESS, "Success")
    }

    override suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse {
        val user = authRepository.findUserByEmail(request.email)
            ?: return ForgotPasswordResponse(ForgotPasswordResult.SUCCESS, "Sent (simulated)")

        emailVerificationService.sendVerificationCodeByEmail(request.email, EmailVerificationType.PASSWORD_RESET)
        return ForgotPasswordResponse(ForgotPasswordResult.SUCCESS, "Sent")
    }

    override suspend fun resetPassword(request: ResetPasswordRequest): ResetPasswordResponse {
        val isValid = emailVerificationService.verifyCodeByEmail(
            request.email,
            request.code,
            EmailVerificationType.PASSWORD_RESET
        )

        if (!isValid) {
            return ResetPasswordResponse(ResetPasswordResult.INVALID_CODE, "Invalid code")
        }

        val user = authRepository.findUserByEmail(request.email)
            ?: return ResetPasswordResponse(ResetPasswordResult.USER_NOT_FOUND, "Not found")

        authRepository.updatePassword(user.id, request.newPassword)
        authRepository.updateRefreshToken(user.id, null)

        return ResetPasswordResponse(ResetPasswordResult.SUCCESS, "Success")
    }
}
