package eric.bitria.hexon.users.profile

import eric.bitria.hexon.dtos.profile.UserProfileResponse

interface UserProfileService {

    /**
     * Fetches the user's profile, calculates statistics, and maps it to the API response.
     * @param userId The ID extracted from the JWT token.
     */
    suspend fun getProfile(userId: String): UserProfileResponse
}