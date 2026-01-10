package eric.bitria.hexon.users.profile

import eric.bitria.hexon.dtos.profile.UserProfileResponse
import eric.bitria.hexon.dtos.profile.UserStats

class UserProfileServiceImpl(
    private val profileRepository: ProfileRepository
) : UserProfileService {

    override suspend fun getProfile(userId: String): UserProfileResponse {
        // 1. Fetch the raw Domain Model from Repository
        val profile = profileRepository.getUserProfile(userId)
            ?: throw IllegalStateException("User profile not found for verified user: $userId")

        // 2. Business Logic: Calculate Win Rate
        val totalGames = profile.gamesWon + profile.gamesLost
        val calculatedWinRate = if (totalGames > 0) {
            (profile.gamesWon.toDouble() / totalGames) * 100
        } else {
            0.0
        }

        // 3. Mapping: Convert Domain Model -> API DTO
        return UserProfileResponse(
            id = profile.userId,
            email = profile.email,
            username = profile.username,
            stats = UserStats(
                wins = profile.gamesWon,
                losses = profile.gamesLost,
                winRate = calculatedWinRate
            )
        )
    }
}