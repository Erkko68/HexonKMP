package eric.bitria.hexon.dtos.social

import kotlinx.serialization.Serializable

@Serializable
data class AddFriendRequest(
    val targetUsername: String
)

@Serializable
data class AddFriendResponse(
    val result: AddFriendResult,
    val message: String? = null
)

@Serializable
enum class AddFriendResult {
    SUCCESS,
    USER_NOT_FOUND,
    ALREADY_FRIENDS,
    REQUEST_ALREADY_SENT,
    CANNOT_ADD_SELF,
    ERROR
}

@Serializable
data class RespondFriendRequest(
    val requesterUsername: String,
    val action: FriendRequestAction
)

@Serializable
enum class FriendRequestAction {
    ACCEPT,
    DECLINE
}

@Serializable
data class RespondFriendResponse(
    val result: RespondFriendResult,
    val message: String? = null
)

@Serializable
enum class RespondFriendResult {
    SUCCESS,
    REQUEST_NOT_FOUND,
    ERROR
}