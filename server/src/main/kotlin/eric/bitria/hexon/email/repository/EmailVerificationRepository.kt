package eric.bitria.hexon.email.repository

import eric.bitria.hexon.database.tables.EmailVerificationType
import kotlin.time.Instant

interface EmailVerificationRepository {

    /**
     * Saves (Upserts) a code.
     * Because 'email' is the PK, this overwrites ANY existing code for this user,
     * ensuring only one active code exists at a time.
     */
    suspend fun saveVerificationCode(
        email: String,
        codeHash: String,
        type: EmailVerificationType,
        expiresAt: Instant
    )

    /**
     * Retrieves the hash ONLY if the email AND the expected type match.
     * Returns null if no code exists, or if the stored code is for a different type.
     */
    suspend fun getVerificationCodeHash(
        email: String,
        requiredType: EmailVerificationType
    ): String?

    /**
     * Increments the attempt counter for the user's active code.
     */
    suspend fun incrementAttempts(email: String)

    /**
     * Deletes the active code for this user.
     */
    suspend fun deleteVerificationCode(email: String)

    /**
     * Maintenance: Deletes all expired codes from the table.
     */
    suspend fun deleteExpiredCodes()
}