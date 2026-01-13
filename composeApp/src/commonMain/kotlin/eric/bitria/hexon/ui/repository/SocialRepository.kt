package eric.bitria.hexon.ui.repository

import eric.bitria.hexon.api.client.SocialClient
import eric.bitria.hexon.dtos.social.*

interface SocialRepository {
    suspend fun getFriends(): ApiResult<GetFriendsResult>
    suspend fun getFriendRequests(): ApiResult<GetFriendRequestsResult>
    suspend fun addFriend(targetUsername: String): ApiResult<AddFriendResult>
    suspend fun respondToRequest(requesterUsername: String, action: String): ApiResult<RespondFriendResult>
}

class SocialRepositoryImpl(
    private val socialClient: SocialClient
) : SocialRepository {

    override suspend fun getFriends(): ApiResult<GetFriendsResult> {
        return safeApiCall {
            socialClient.getFriends().result
        }
    }

    override suspend fun getFriendRequests(): ApiResult<GetFriendRequestsResult> {
        return safeApiCall {
            socialClient.getFriendRequests().result
        }
    }

    override suspend fun addFriend(targetUsername: String): ApiResult<AddFriendResult> {
        return safeApiCall {
            socialClient.addFriend(AddFriendRequest(targetUsername)).result
        }
    }

    override suspend fun respondToRequest(requesterUsername: String, action: String): ApiResult<RespondFriendResult> {
        return safeApiCall {
            val requestAction = when (action) {
                "accept" -> FriendRequestAction.ACCEPT
                "decline" -> FriendRequestAction.DECLINE
                else -> throw IllegalArgumentException("Invalid action")
            }
            socialClient.respondToFriendRequest(RespondFriendRequest(requesterUsername, requestAction)).result
        }
    }
}
