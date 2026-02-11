package eric.bitria.hexon.social.mock

import eric.bitria.hexon.services.social.repository.Friend
import eric.bitria.hexon.services.social.repository.FriendRequestRepository

class MockFriendRequestRepository : FriendRequestRepository {
    private val requests = mutableListOf<Pair<String, String>>() // (requesterId, targetId)

    override suspend fun createRequest(requesterId: String, receiverId: String): Boolean {
        requests.add(Pair(requesterId, receiverId))
        return true
    }

    override suspend fun deleteRequest(requesterId: String, receiverId: String): Boolean {
        requests.removeIf { it.first == requesterId && it.second == receiverId }
        return true
    }

    override suspend fun hasPendingRequest(requesterId: String, receiverId: String): Boolean {
        return requests.any { it.first == requesterId && it.second == receiverId }
    }

    override suspend fun getIncomingRequests(receiverId: String): List<Friend> {
        // For simplicity, return empty list in tests
        return emptyList()
    }
}

