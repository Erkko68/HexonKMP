package eric.bitria.hexon.client

import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResponse
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse

interface UserClient {
    suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse
    suspend fun resendVerificationCode(request: ResendVerificationCodeRequest): ResendVerificationCodeResponse

    suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse
    // suspend fun forgotPassword(...)
}