package eric.bitria.hexon.email.verification

import eric.bitria.hexon.dtos.auth.EmailVerificationType

interface EmailVerificationService {

    /**
     * Use this when you have the raw email address (e.g., Forgot Password).
     */
    suspend fun sendVerificationCodeByEmail(
        email: String,
        type: EmailVerificationType
    )

    /**
     * Use this when you have the User ID (e.g., Account Deletion).
     * The service will look up the email internally.
     */
    suspend fun sendVerificationCodeByUserId(
        userId: String,
        type: EmailVerificationType
    )

    /**
     * Verifies code using the email directly.
     */
    suspend fun verifyCodeByEmail(
        email: String,
        code: String,
        type: EmailVerificationType
    ): Boolean

    /**
     * Verifies code using the User ID (looks up email internally).
     */
    suspend fun verifyCodeByUserId(
        userId: String,
        code: String,
        type: EmailVerificationType
    ): Boolean
}