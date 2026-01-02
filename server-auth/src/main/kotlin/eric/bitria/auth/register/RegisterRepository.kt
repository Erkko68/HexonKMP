package eric.bitria.auth.register

import eric.bitria.hexon.dtos.auth.VerifyEmailResult

/**
 * Repository interface for managing user registration and email verification.
 * Abstracts persistence layer (in-memory, database, etc.).
 */
interface RegisterRepository {

    /**
     * Checks if a username is already taken.
     */
    fun usernameExists(username: String): Boolean

    /**
     * Checks if an email is already registered.
     */
    fun emailExists(email: String): Boolean

    /**
     * Checks if an email is verified.
     */
    fun isAccountVerified(email: String): Boolean

    /**
     * Saves a new user along with the verification code.
     */
    fun saveUser(email: String, username: String, password: String, verificationCode: String)

    /**
     * Marks the email as verified if the code matches.
     * Returns true if verification succeeded, false otherwise.
     */
    fun verifyEmail(email: String, code: String): VerifyEmailResult

    /**
     * Updates the verification code for a given email.
     */
    fun updateVerificationCode(email: String, verificationCode: String)
}
