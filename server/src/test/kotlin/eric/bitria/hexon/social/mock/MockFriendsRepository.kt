package eric.bitria.hexon.social.mock

import eric.bitria.hexon.services.social.repository.Friend
import eric.bitria.hexon.services.social.repository.FriendsRepository

class MockFriendsRepository : FriendsRepository {
    private val friendships = mutableListOf<Pair<String, String>>()

    override suspend fun addFriendship(userId1: String, userId2: String): Boolean {
        friendships.add(Pair(userId1, userId2))
        return true
    }

    override suspend fun removeFriendship(userId1: String, userId2: String): Boolean {
        friendships.removeIf {
            (it.first == userId1 && it.second == userId2) ||
            (it.first == userId2 && it.second == userId1)
        }
        return true
    }

    override suspend fun areFriends(userId1: String, userId2: String): Boolean {
        return friendships.any {
            (it.first == userId1 && it.second == userId2) ||
            (it.first == userId2 && it.second == userId1)
        }
    }

    override suspend fun getFriendsForUser(userId: String): List<Friend> {
        // For simplicity, just return empty list in tests
        return emptyList()
    }
}

