package eric.bitria.auth.database

import eric.bitria.hexon.dtos.auth.VerifyEmailResult

interface AuthRepository {

    // --- Existence Checks ---

    /**
     * Checks if a username is already taken.
     */
    suspend fun usernameExists(username: String): Boolean

    /**
     * Checks if an email is already registered.
     */
    suspend fun emailExists(email: String): Boolean

    // --- Account Verification ---

    /**
     * Checks if an email is verified.
     */
    suspend fun isAccountVerified(email: String): Boolean

    /**
     * Marks the email as verified if the code matches.
     * Returns a result indicating success or failure.
     */
    suspend fun verifyEmail(email: String, code: String): VerifyEmailResult

    /**
     * Updates the verification code for a given email.
     */
    suspend fun updateVerificationCode(email: String, verificationCode: String)

    // --- User Management ---

    /**
     * Saves a new user along with the verification code.
     */
    suspend fun saveUser(email: String, username: String, password: String, verificationCode: String)

    /**
     * Retrieves the user ID associated with the given email.
     */
    suspend fun getUserIdByEmail(email: String): String

    // --- Authentication ---

    /**
     * Retrieves the hashed password for the given email.
     */
    suspend fun getPasswordByEmail(email: String): String?
}
