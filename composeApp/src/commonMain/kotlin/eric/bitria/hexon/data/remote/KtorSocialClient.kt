package eric.bitria.hexon.data.remote

import eric.bitria.hexon.dtos.social.AddFriendRequest
import eric.bitria.hexon.dtos.social.AddFriendResponse
import eric.bitria.hexon.dtos.social.GetFriendsResponse
import eric.bitria.hexon.dtos.social.GetFriendRequestsResponse
import eric.bitria.hexon.dtos.social.RespondFriendRequest
import eric.bitria.hexon.dtos.social.RespondFriendResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

interface SocialClient {
    suspend fun getFriends(): GetFriendsResponse
    suspend fun getFriendRequests(): GetFriendRequestsResponse
    suspend fun addFriend(request: AddFriendRequest): AddFriendResponse
    suspend fun respondToFriendRequest(request: RespondFriendRequest): RespondFriendResponse
}


class KtorSocialClient(
    private val client: HttpClient
) : SocialClient {

    override suspend fun getFriends(): GetFriendsResponse {
        return client.get("/friends").body()
    }

    override suspend fun getFriendRequests(): GetFriendRequestsResponse {
        return client.get("/friends/requests").body()
    }

    override suspend fun addFriend(request: AddFriendRequest): AddFriendResponse {
        return client.post("/friends/add") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun respondToFriendRequest(request: RespondFriendRequest): RespondFriendResponse {
        return client.post("/friends/respond") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
