package eric.bitria.hexon.auth.register

import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse


interface AccountVerificationService {

    /**
     * Verifies the email confirmation code.
     * If successful, marks the user as 'verified' in the database.
     */
    suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse
}