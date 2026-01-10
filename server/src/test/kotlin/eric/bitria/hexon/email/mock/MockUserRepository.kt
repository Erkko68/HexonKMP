package eric.bitria.hexon.email.mock

import eric.bitria.hexon.users.repository.UserRepository

class MockUserRepository : UserRepository {
    private val userIdToEmail = mutableMapOf<String, String>()
    private val emailToUserId = mutableMapOf<String, String>()

    fun addUser(userId: String, email: String) {
        userIdToEmail[userId] = email
        emailToUserId[email] = userId
    }

    override suspend fun getEmailByUserId(userId: String): String? {
        return userIdToEmail[userId]
    }

    override suspend fun getUserIdByEmail(email: String): String? {
        return emailToUserId[email]
    }
}
