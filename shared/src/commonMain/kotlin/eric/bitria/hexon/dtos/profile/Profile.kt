package eric.bitria.hexon.dtos.profile

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse( // Private (For /me)
    val id: String,
    val email: String,
    val username: String,
    val stats: UserStats
)

@Serializable
data class PublicUserProfileResponse( // Public (For /users/{id})
    val id: String,
    val username: String,
    val stats: UserStats
)

@Serializable
data class UserStats(
    val wins: Int,
    val losses: Int
)