package eric.bitria.hexon.services.users.verify

import eric.bitria.hexon.dtos.auth.ResendVerificationCodeRequest
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResponse
import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse


interface AccountVerificationService {

    /**
     * Verifies the email confirmation code.
     * If successful, marks the user as 'verified' in the database.
     */
    suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse

    /**
     * Resends a verification code to the user.
     * 1. Checks if user exists.
     * 2. Checks if user is already verified.
     * 3. Generates a new code and emails it (overwriting the old one).
     */
    suspend fun resendVerificationCode(request: ResendVerificationCodeRequest): ResendVerificationCodeResponse
}