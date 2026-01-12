package eric.bitria.hexon.dtos.social

import kotlinx.serialization.Serializable

@Serializable
data class FriendDto(
    val id: String,
    val username: String,
    val isOnline: Boolean
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
    UNKNOWN_ERROR
}

@Serializable
data class GetFriendRequestsResponse(
    val result: GetFriendRequestsResult,
    val requests: List<FriendDto> = emptyList(),
    val message: String? = null
)

@Serializable
enum class GetFriendRequestsResult {
    SUCCESS,
    UNKNOWN_ERROR
}
