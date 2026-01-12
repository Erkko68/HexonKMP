package eric.bitria.hexon.services.social

import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.social.AddFriendResponse
import eric.bitria.hexon.dtos.social.AddFriendResult
import eric.bitria.hexon.dtos.social.FriendDto
import eric.bitria.hexon.dtos.social.FriendRequestAction
import eric.bitria.hexon.dtos.social.GetFriendsResponse
import eric.bitria.hexon.dtos.social.GetFriendsResult
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

            // Map Domain Model -> Network DTO
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

    // --- 2. Send Friend Request ---
    override suspend fun sendFriendRequest(requesterId: String, targetUsername: String): AddFriendResponse {
        try {
            // A. Check if target user exists
            val targetUser = authRepository.findUserByUsername(targetUsername)
                ?: return AddFriendResponse(AddFriendResult.USER_NOT_FOUND)

            // B. Validation Rules
            if (requesterId == targetUser.id) {
                return AddFriendResponse(AddFriendResult.CANNOT_ADD_SELF)
            }

            // Check if already friends (Direction A -> B is enough to check connection)
            if (friendsRepository.areFriends(requesterId, targetUser.id)) {
                return AddFriendResponse(AddFriendResult.ALREADY_FRIENDS)
            }

            // Check if request already pending (Prevent spam)
            if (requestsRepository.hasPendingRequest(requesterId, targetUser.id)) {
                return AddFriendResponse(AddFriendResult.REQUEST_ALREADY_SENT)
            }

            // C. Create the Request
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
        userId: String, // Me (The Receiver)
        requesterUsername: String, // The Sender
        action: FriendRequestAction
    ): RespondFriendResponse {
        try {
            // A. Resolve the Requester's ID
            val requester = authRepository.findUserByUsername(requesterUsername)
                ?: return RespondFriendResponse(RespondFriendResult.REQUEST_NOT_FOUND)

            // B. Verify the request actually exists (Security check)
            // Logic: Does a request exist FROM requester TO me?
            val hasRequest = requestsRepository.hasPendingRequest(requester.id, userId)
            if (!hasRequest) {
                return RespondFriendResponse(RespondFriendResult.REQUEST_NOT_FOUND)
            }

            // C. Perform Action
            when (action) {
                FriendRequestAction.ACCEPT -> {
                    // 1. Create Mutual Friendship
                    // We add BOTH directions so "getFriends" works for both users
                    val addedForward = friendsRepository.addFriendship(userId, requester.id)
                    val addedBackward = friendsRepository.addFriendship(requester.id, userId)

                    if (addedForward && addedBackward) {
                        // 2. Delete the pending request
                        requestsRepository.deleteRequest(requester.id, userId)
                        return RespondFriendResponse(RespondFriendResult.SUCCESS)
                    } else {
                        return RespondFriendResponse(RespondFriendResult.UNKNOWN_ERROR, "Failed to create friendship links")
                    }
                }

                FriendRequestAction.DECLINE -> {
                    // Just delete the request
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