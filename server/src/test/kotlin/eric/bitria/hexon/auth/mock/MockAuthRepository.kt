package eric.bitria.hexon.auth.mock

import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.auth.repository.User

class MockAuthRepository : AuthRepository {
    private val users = mutableMapOf<String, User>()

    fun addUser(user: User) {
        users[user.id] = user
    }

    override suspend fun isEmailRegistered(email: String): Boolean = users.values.any { it.email == email }

    override suspend fun isUsernameTaken(username: String): Boolean = users.values.any { it.username == username }

    override suspend fun createUser(email: String, username: String, passwordHash: String): User {
        val user = User(
            id = "user-${users.size + 1}",
            email = email,
            username = username,
            password = passwordHash,
            isVerified = false,
            refreshTokenHash = null
        )
        users[user.id] = user
        return user
    }

    override suspend fun findUserByEmail(email: String): User? = users.values.find { it.email == email }

    override suspend fun findUserById(userId: String): User? = users[userId]

    override suspend fun updateRefreshToken(userId: String, refreshTokenHash: String?) {
        val user = users[userId] ?: return
        users[userId] = user.copy(refreshTokenHash = refreshTokenHash)
    }

    override suspend fun getRefreshTokenHash(userId: String): String? = users[userId]?.refreshTokenHash

    override suspend fun verifyUser(userId: String) {
        val user = users[userId] ?: return
        users[userId] = user.copy(isVerified = true)
    }

    override suspend fun updatePassword(userId: String, newPasswordHash: String) {
        val user = users[userId] ?: return
        users[userId] = user.copy(password = newPasswordHash)
    }

    override suspend fun deleteUser(userId: String) {
        users.remove(userId)
    }
}
