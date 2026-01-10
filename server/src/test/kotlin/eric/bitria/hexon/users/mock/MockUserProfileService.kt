package eric.bitria.hexon.users.mock

import eric.bitria.hexon.dtos.profile.UserProfileResponse
import eric.bitria.hexon.users.profile.UserProfileService

class MockUserProfileService : UserProfileService {
    override suspend fun getProfile(userId: String): UserProfileResponse {
        TODO("Not yet implemented")
    }
}