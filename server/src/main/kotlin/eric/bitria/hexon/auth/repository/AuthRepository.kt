package eric.bitria.hexon.auth.repository

import eric.bitria.hexon.dtos.auth.VerifyEmailResult

interface AuthRepository {

    // --- Existence Checks ---

    suspend fun usernameExists(username: String): Boolean

    suspend fun emailExists(email: String): Boolean

    // --- Account Verification ---

    suspend fun isAccountVerified(email: String): Boolean

    suspend fun verifyEmail(email: String, code: String): VerifyEmailResult

    suspend fun updateVerificationCode(email: String, verificationCode: String)

    // --- User Management ---

    /**
     * Saves a new user or updates an existing unverified user.
     * Implementation should check if the email exists and is NOT verified before updating.
     */
    suspend fun saveOrUpdateUnverifiedUser(
        email: String, 
        username: String, 
        password: String, 
        verificationCode: String
    )

    suspend fun getUserIdByEmail(email: String): String

    suspend fun getEmailByUsername(username: String): String?

    // --- Authentication ---

    suspend fun getPasswordByEmail(email: String): String?
}
