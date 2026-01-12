package eric.bitria.hexon.users.mock

import eric.bitria.hexon.dtos.profile.PublicUserProfileResponse
import eric.bitria.hexon.dtos.profile.UserProfileResponse
import eric.bitria.hexon.services.users.profile.UserProfileService

class MockUserProfileService : UserProfileService {
    override suspend fun getMyProfile(userId: String): UserProfileResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getPublicProfile(userId: String): PublicUserProfileResponse? {
        TODO("Not yet implemented")
    }
}