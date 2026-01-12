package eric.bitria.hexon.dtos.social

import kotlinx.serialization.Serializable

@Serializable
data class FriendDto(
    val id: String,
    val username: String,
    val isOnline: Boolean,
    val avatarUrl: String? = null
)

// --- Feature 1: Get Friends ---
@Serializable
data class GetFriendsResponse(
    val result: GetFriendsResult,
    val friends: List<FriendDto> = emptyList(),
    val message: String? = null
)

@Serializable
enum class GetFriendsResult {
    SUCCESS,
    ERROR
}