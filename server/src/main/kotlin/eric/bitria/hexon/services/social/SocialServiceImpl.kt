package eric.bitria.hexon.services.social

import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.social.AddFriendResponse
import eric.bitria.hexon.dtos.social.AddFriendResult
import eric.bitria.hexon.dtos.social.FriendDto
import eric.bitria.hexon.dtos.social.FriendRequestAction
import eric.bitria.hexon.dtos.social.GetFriendsResponse
import eric.bitria.hexon.dtos.social.GetFriendsResult
import eric.bitria.hexon.dtos.social.GetFriendRequestsResponse
import eric.bitria.hexon.dtos.social.GetFriendRequestsResult
import eric.bitria.hexon.dtos.social.RespondFriendResponse
import eric.bitria.hexon.dtos.social.RespondFriendResult
import eric.bitria.hexon.services.social.repository.FriendRequestRepository
import eric.bitria.hexon.services.social.repository.FriendsRepository

class SocialServiceImpl(
    private val friendsRepository: FriendsRepository,
    private val requestsRepository: FriendRequestRepository,
    private val authRepository: AuthRepository
) : SocialService {

    // --- 1. Get Friends ---
    override suspend fun getFriends(userId: String): GetFriendsResponse {
        return try {
            val dbFriends = friendsRepository.getFriendsForUser(userId)

            val friendDtos = dbFriends.map { friend ->
                FriendDto(
                    id = friend.id,
                    username = friend.username,
                    isOnline = friend.isOnline,
                )
            }

            GetFriendsResponse(GetFriendsResult.SUCCESS, friends = friendDtos)
        } catch (e: Exception) {
            e.printStackTrace()
            GetFriendsResponse(GetFriendsResult.UNKNOWN_ERROR, message = "Failed to load friends")
        }
    }

    override suspend fun getFriendRequests(userId: String): GetFriendRequestsResponse {
        return try {
            val dbRequests = requestsRepository.getIncomingRequests(userId)

            val friendDtos = dbRequests.map { friend ->
                FriendDto(
                    id = friend.id,
                    username = friend.username,
                    isOnline = friend.isOnline,
                )
            }

            GetFriendRequestsResponse(GetFriendRequestsResult.SUCCESS, requests = friendDtos)
        } catch (e: Exception) {
            e.printStackTrace()
            GetFriendRequestsResponse(GetFriendRequestsResult.UNKNOWN_ERROR, message = "Failed to load friend requests")
        }
    }

    // --- 2. Send Friend Request ---
    override suspend fun sendFriendRequest(requesterId: String, targetUsername: String): AddFriendResponse {
        try {
            val targetUser = authRepository.findUserByUsername(targetUsername)
                ?: return AddFriendResponse(AddFriendResult.USER_NOT_FOUND)

            if (requesterId == targetUser.id) {
                return AddFriendResponse(AddFriendResult.CANNOT_ADD_SELF)
            }

            if (friendsRepository.areFriends(requesterId, targetUser.id)) {
                return AddFriendResponse(AddFriendResult.ALREADY_FRIENDS)
            }

            if (requestsRepository.hasPendingRequest(requesterId, targetUser.id)) {
                return AddFriendResponse(AddFriendResult.REQUEST_ALREADY_SENT)
            }

            val success = requestsRepository.createRequest(requesterId, targetUser.id)

            return if (success) {
                AddFriendResponse(AddFriendResult.SUCCESS)
            } else {
                AddFriendResponse(AddFriendResult.UNKNOWN_ERROR, "Database insert failed")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return AddFriendResponse(AddFriendResult.UNKNOWN_ERROR, e.message)
        }
    }

    // --- 3. Respond to Request (Accept/Decline) ---
    override suspend fun respondToRequest(
        userId: String,
        requesterUsername: String,
        action: FriendRequestAction
    ): RespondFriendResponse {
        try {
            val requester = authRepository.findUserByUsername(requesterUsername)
                ?: return RespondFriendResponse(RespondFriendResult.REQUEST_NOT_FOUND)

            val hasRequest = requestsRepository.hasPendingRequest(requester.id, userId)
            if (!hasRequest) {
                return RespondFriendResponse(RespondFriendResult.REQUEST_NOT_FOUND)
            }

            when (action) {
                FriendRequestAction.ACCEPT -> {
                    val addedForward = friendsRepository.addFriendship(userId, requester.id)
                    val addedBackward = friendsRepository.addFriendship(requester.id, userId)

                    if (addedForward && addedBackward) {
                        requestsRepository.deleteRequest(requester.id, userId)
                        return RespondFriendResponse(RespondFriendResult.SUCCESS)
                    } else {
                        return RespondFriendResponse(RespondFriendResult.UNKNOWN_ERROR, "Failed to create friendship links")
                    }
                }

                FriendRequestAction.DECLINE -> {
                    val deleted = requestsRepository.deleteRequest(requester.id, userId)
                    return if (deleted) {
                        RespondFriendResponse(RespondFriendResult.SUCCESS)
                    } else {
                        RespondFriendResponse(RespondFriendResult.UNKNOWN_ERROR, "Failed to decline request")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return RespondFriendResponse(RespondFriendResult.UNKNOWN_ERROR, e.message)
        }
    }
}
