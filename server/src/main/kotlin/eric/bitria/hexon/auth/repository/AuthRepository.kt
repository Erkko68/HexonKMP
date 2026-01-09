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

    // --- User Management ---

    suspend fun saveOrUpdateUnverifiedUser(
        email: String, 
        username: String, 
        password: String, 
        verificationCode: String
    )

    suspend fun getUserIdByEmail(email: String): String?

    suspend fun getEmailByUsername(username: String): String?

    // --- Authentication ---

    suspend fun getPasswordByUserId(userId: String): String?

    // --- Password Reset ---
    suspend fun updatePasswordByUserId(userId: String, passwordHash: String)

    suspend fun updateUserCodeByEmail(email: String, resetCode: String)

    suspend fun getUserCodeByUserId(userId: String): String?

    suspend fun clearUserCode(email: String)

    // --- Refresh Token Management ---
    suspend fun updateRefreshTokenHash(userId: String, hash: String)
    suspend fun getRefreshTokenHash(userId: String): String?
}
