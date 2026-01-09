package eric.bitria.hexon.users.verify

import eric.bitria.hexon.dtos.auth.VerifyEmailRequest
import eric.bitria.hexon.dtos.auth.VerifyEmailResponse


interface UserVerificationService {

    /**
     * Verifies the email confirmation code.
     * If successful, marks the user as 'verified' in the database.
     */
    suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse
}