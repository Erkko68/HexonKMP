package eric.bitria.hexon.services.users.profile

import eric.bitria.hexon.dtos.profile.PublicUserProfileResponse
import eric.bitria.hexon.dtos.profile.UserProfileResponse
import eric.bitria.hexon.dtos.profile.UserStats

class UserProfileServiceImpl(
    private val profileRepository: ProfileRepository
) : UserProfileService {

    override suspend fun getMyProfile(userId: String): UserProfileResponse {
        val profile = profileRepository.getUserProfile(userId)
            ?: throw IllegalStateException("User profile not found")

        return UserProfileResponse(
            id = profile.userId,
            email = profile.email,
            username = profile.username,
            stats = UserStats(profile.gamesWon, profile.gamesLost)
        )
    }

    override suspend fun getPublicProfile(userId: String): PublicUserProfileResponse? {
        val profile = profileRepository.getUserProfile(userId)
            ?: return null // Standard return for "Not Found"

        return PublicUserProfileResponse(
            id = profile.userId,
            username = profile.username,
            stats = UserStats(profile.gamesWon, profile.gamesLost)
        )
    }
}