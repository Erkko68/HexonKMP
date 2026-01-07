package eric.bitria.hexon.auth.repository

interface AuthRepository {

    // --- Existence Checks ---

    suspend fun usernameExists(username: String): Boolean

    suspend fun emailExists(email: String): Boolean

    // --- Account Verification ---

    suspend fun isAccountVerified(email: String): Boolean

    suspend fun getVerificationCodeByEmail(email: String): String?

    // Mark as verified once the service confirms the code is correct
    suspend fun markAccountAsVerified(email: String)

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

    // --- Password Reset ---
    suspend fun updatePassword(email: String, passwordHash: String)

    suspend fun updateResetCode(email: String, resetCode: String)

    suspend fun getResetCodeByEmail(email: String): String?

    suspend fun clearResetCode(email: String)
}
