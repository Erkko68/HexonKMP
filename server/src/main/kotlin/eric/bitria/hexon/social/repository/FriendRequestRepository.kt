package eric.bitria.hexon.social.repository

interface FriendRequestRepository {
    // Read
    suspend fun hasPendingRequest(requesterId: String, receiverId: String): Boolean
    suspend fun getIncomingRequests(receiverId: String): List<Friend>

    // Write
    suspend fun createRequest(requesterId: String, receiverId: String): Boolean
    suspend fun deleteRequest(requesterId: String, receiverId: String): Boolean
}