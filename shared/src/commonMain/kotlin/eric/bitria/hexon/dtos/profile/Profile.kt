package eric.bitria.hexon.dtos.profile

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    val id: String,
    val email: String,
    val username: String,
    val stats: UserStats // We Group Stats for Cleaner Json and expansion
)

@Serializable
data class UserStats(
    val wins: Int,
    val losses: Int,
    val winRate: Double
)