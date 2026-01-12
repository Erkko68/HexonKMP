package eric.bitria.hexon.services.auth.repository

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
     * Updates the refresh token hash for a specific user.
     * Used by LoginService (to set initial token) and RefreshService (rotation).
     * @param refreshTokenHash Nullable because logging out would set this to null.
     */
    suspend fun updateRefreshToken(userId: String, refreshTokenHash: String?)

    /**
     * Retrieves the stored refresh token hash for validation.
     * Used by RefreshService to check if the incoming token matches the DB.
     */
    suspend fun getRefreshTokenHash(userId: String): String?

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