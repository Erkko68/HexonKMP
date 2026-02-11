package eric.bitria.hexon.data.repository

import eric.bitria.hexon.data.remote.SocialClient
import eric.bitria.hexon.dtos.social.AddFriendRequest
import eric.bitria.hexon.dtos.social.AddFriendResult
import eric.bitria.hexon.dtos.social.FriendRequestAction
import eric.bitria.hexon.dtos.social.GetFriendRequestsResponse
import eric.bitria.hexon.dtos.social.GetFriendsResponse
import eric.bitria.hexon.dtos.social.RespondFriendRequest
import eric.bitria.hexon.dtos.social.RespondFriendResult

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
