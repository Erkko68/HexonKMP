package eric.bitria.hexon.client.repository

import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse

interface UserClient {
    suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse
    suspend fun resendVerificationCode(request: ResendVerificationCodeRequest): ResendVerificationCodeResponse

    // Future expansion:
    // suspend fun changePassword(...)
    // suspend fun forgotPassword(...)
}