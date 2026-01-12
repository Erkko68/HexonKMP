package eric.bitria.hexon.services.social

import eric.bitria.hexon.dtos.social.AddFriendResponse
import eric.bitria.hexon.dtos.social.FriendRequestAction
import eric.bitria.hexon.dtos.social.GetFriendsResponse
import eric.bitria.hexon.dtos.social.GetFriendRequestsResponse
import eric.bitria.hexon.dtos.social.RespondFriendResponse


interface SocialService {

    /**
     * Retrieves the list of friends for a specific user.
     *
     * @param userId The unique ID of the user requesting their friends list.
     * @return [GetFriendsResponse] containing the list of [FriendDto] or an error status.
     */
    suspend fun getFriends(userId: String): GetFriendsResponse

    /**
     * Retrieves the list of incoming friend requests for a specific user.
     *
     * @param userId The unique ID of the user requesting their friend requests.
     * @return [GetFriendRequestsResponse] containing the list of [FriendDto] or an error status.
     */
    suspend fun getFriendRequests(userId: String): GetFriendRequestsResponse

    /**
     * Processes a request to add a new friend.
     * Checks if the target user exists, if they are already friends, or if a request is pending.
     *
     * @param requesterId The ID of the user sending the friend request (the "Principal").
     * @param targetUsername The username of the person they want to add.
     * @return [AddFriendResponse] indicating if the request was sent, or why it failed (e.g., USER_NOT_FOUND).
     */
    suspend fun sendFriendRequest(requesterId: String, targetUsername: String): AddFriendResponse

    /**
     * Handles the user's decision to Accept or Decline an incoming friend request.
     *
     * @param userId The ID of the user who received the request (the "Principal").
     * @param requesterUsername The username of the person who sent the original request.
     * @param action The decision: [FriendRequestAction.ACCEPT] or [FriendRequestAction.DECLINE].
     * @return [RespondFriendResponse] indicating success or if the original request was missing.
     */
    suspend fun respondToRequest(
        userId: String,
        requesterUsername: String,
        action: FriendRequestAction
    ): RespondFriendResponse
}