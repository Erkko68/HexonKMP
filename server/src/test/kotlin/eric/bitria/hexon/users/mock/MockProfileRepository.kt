package eric.bitria.hexon.users.mock

import eric.bitria.hexon.services.users.profile.ProfileRepository
import eric.bitria.hexon.services.users.profile.UserProfile

class MockProfileRepository : ProfileRepository {
    private val profiles = mutableMapOf<String, UserProfile>()

    fun addProfile(profile: UserProfile) {
        profiles[profile.userId] = profile
    }

    override suspend fun createProfile(userId: String) {
        if (!profiles.containsKey(userId)) {
            profiles[userId] = UserProfile(
                userId = userId,
                email = "",
                username = "",
                gamesWon = 0,
                gamesLost = 0
            )
        }
    }

    override suspend fun getUserProfile(userId: String): UserProfile? {
        return profiles[userId]
    }

    override suspend fun updateStats(userId: String, isWin: Boolean) {
        val existing = profiles[userId] ?: return
        profiles[userId] = if (isWin) {
            existing.copy(gamesWon = existing.gamesWon + 1)
        } else {
            existing.copy(gamesLost = existing.gamesLost + 1)
        }
    }
}

