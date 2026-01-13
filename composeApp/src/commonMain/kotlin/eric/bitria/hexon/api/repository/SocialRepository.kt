package eric.bitria.hexon.api.repository

import eric.bitria.hexon.api.client.SocialClient
import eric.bitria.hexon.dtos.social.*

interface SocialRepository {
    suspend fun getFriends(): ApiResult<GetFriendsResponse>
    suspend fun getFriendRequests(): ApiResult<GetFriendRequestsResponse>
    suspend fun addFriend(targetUsername: String): ApiResult<AddFriendResult>
    suspend fun respondToRequest(requesterUsername: String, action: FriendRequestAction): ApiResult<RespondFriendResult>
}

class SocialRepositoryImpl(
    private val socialClient: SocialClient
) : SocialRepository {

    override suspend fun getFriends(): ApiResult<GetFriendsResponse> {
        return safeApiCall {
            socialClient.getFriends()
        }
    }

    override suspend fun getFriendRequests(): ApiResult<GetFriendRequestsResponse> {
        return safeApiCall {
            socialClient.getFriendRequests()
        }
    }

    override suspend fun addFriend(targetUsername: String): ApiResult<AddFriendResult> {
        return safeApiCall {
            socialClient.addFriend(AddFriendRequest(targetUsername)).result
        }
    }

    override suspend fun respondToRequest(requesterUsername: String, action: FriendRequestAction): ApiResult<RespondFriendResult> {
        return safeApiCall {
            socialClient.respondToFriendRequest(RespondFriendRequest(requesterUsername, action)).result
        }
    }
}
