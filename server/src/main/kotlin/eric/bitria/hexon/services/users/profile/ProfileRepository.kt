package eric.bitria.hexon.services.users.profile

interface ProfileRepository {

    /**
     * Creates an empty profile row for a new user.
     * Should be called immediately after Email Verification.
     */
    suspend fun createProfile(userId: String)

    /**
     * Fetches the full user profile (User info + Stats).
     */
    suspend fun getUserProfile(userId: String): UserProfile?

    /**
     * Updates the win/loss stats.
     * Use this after a game finishes.
     */
    suspend fun updateStats(userId: String, isWin: Boolean)
}