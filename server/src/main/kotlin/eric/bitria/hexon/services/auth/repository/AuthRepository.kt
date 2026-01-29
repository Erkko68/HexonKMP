package eric.bitria.hexon.services.auth.repository

import java.time.LocalDateTime

interface AuthRepository {

    /**
     * Checks if a user already exists with the given email or username.
     * Used during Registration to prevent duplicates.
     */
    suspend fun isEmailRegistered(email: String): Boolean
    suspend fun isUsernameTaken(username: String): Boolean

    /**
     * Creates a new user in the database.
     * Used by RegisterService.
     * @return The newly created User object (including generated ID).
     */
    suspend fun createUser(
        email: String,
        username: String,
        passwordHash: String
    ): User

    /**
     * Finds a user by their email.
     * Used by LoginService to verify credentials.
     */
    suspend fun findUserByEmail(email: String): User?

    /**
     * Finds a user by their ID.
     * Used by RefreshService (and general app usage).
     */
    suspend fun findUserById(userId: String): User?

    /**
     * Finds a user by their username.
     */
    suspend fun findUserByUsername(username: String): User?

    /**
     * Adds a new refresh token for a specific user.
     * Supports multiple active sessions.
     */
    suspend fun addRefreshToken(userId: String, refreshTokenHash: String, expiresAt: LocalDateTime)

    /**
     * Updates an existing refresh token (used for token rotation).
     * Replaces oldHash with newHash.
     */
    suspend fun updateRefreshToken(oldHash: String, newHash: String, newExpiresAt: LocalDateTime): Boolean

    /**
     * Retrieves the stored refresh token hash for validation.
     * Used by RefreshService to check if the incoming token exists.
     */
    suspend fun hasRefreshTokenHash(refreshTokenHash: String): Boolean

    /**
     * Revokes a specific session.
     */
    suspend fun revokeRefreshToken(refreshTokenHash: String)

    /**
     * Revokes all sessions for a specific user.
     */
    suspend fun revokeAllRefreshTokens(userId: String)

    /**
     * Deletes all expired sessions from the database.
     */
    suspend fun clearExpiredSessions()

    /**
     * Marks a user as verified.
     * Used by AccountVerificationService.
     */
    suspend fun verifyUser(userId: String)

    /**
     * Updates the password hash for a specific user.
     * Used by ChangePasswordService.
     */
    suspend fun updatePassword(userId: String, newPasswordHash: String)

    /**
     * Deletes a user from the database.
     * Used by AccountDeletionService.
     */
    suspend fun deleteUser(userId: String)
}
