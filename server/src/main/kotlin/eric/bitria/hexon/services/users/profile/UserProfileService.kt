package eric.bitria.hexon.services.users.profile

import eric.bitria.hexon.dtos.profile.PublicUserProfileResponse
import eric.bitria.hexon.dtos.profile.UserProfileResponse

interface UserProfileService {
    // Throws exception if not found (because /me should always exist)
    suspend fun getMyProfile(userId: String): UserProfileResponse

    // Returns null if not found
    suspend fun getPublicProfile(userId: String): PublicUserProfileResponse?
}