package eric.bitria.hexon.client

import eric.bitria.hexon.dtos.social.AddFriendRequest
import eric.bitria.hexon.dtos.social.AddFriendResponse
import eric.bitria.hexon.dtos.social.GetFriendsResponse
import eric.bitria.hexon.dtos.social.GetFriendRequestsResponse
import eric.bitria.hexon.dtos.social.RespondFriendRequest
import eric.bitria.hexon.dtos.social.RespondFriendResponse

interface SocialClient {
    suspend fun getFriends(): GetFriendsResponse
    suspend fun getFriendRequests(): GetFriendRequestsResponse
    suspend fun addFriend(request: AddFriendRequest): AddFriendResponse
    suspend fun respondToFriendRequest(request: RespondFriendRequest): RespondFriendResponse
}
