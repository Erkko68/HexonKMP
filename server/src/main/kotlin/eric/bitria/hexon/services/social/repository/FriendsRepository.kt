package eric.bitria.hexon.services.social.repository

interface FriendsRepository {
    // Read
    suspend fun getFriendsForUser(userId: String): List<Friend>
    suspend fun areFriends(userId1: String, userId2: String): Boolean

    // Write
    suspend fun addFriendship(userId1: String, userId2: String): Boolean
    suspend fun removeFriendship(userId1: String, userId2: String): Boolean
}