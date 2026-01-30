package eric.bitria.hexon.auth.mock

import eric.bitria.hexon.services.auth.repository.AuthRepository
import eric.bitria.hexon.services.auth.repository.User
import java.time.LocalDateTime

class MockAuthRepository : AuthRepository {
    private val users = mutableMapOf<String, User>()
    private val sessions = mutableListOf<MockSession>()

    data class MockSession(
        val userId: String,
        var refreshTokenHash: String,
        var expiresAt: LocalDateTime
    )

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
        )
        users[user.id] = user
        return user
    }

    override suspend fun findUserByEmail(email: String): User? = users.values.find { it.email == email }

    override suspend fun findUserById(userId: String): User? = users[userId]
    override suspend fun findUserByUsername(username: String): User? = users.values.find { it.username == username }

    override suspend fun addRefreshToken(
        userId: String,
        refreshTokenHash: String,
        expiresAt: LocalDateTime
    ) {
        sessions.add(MockSession(userId, refreshTokenHash, expiresAt))
    }

    override suspend fun updateRefreshToken(
        oldHash: String,
        newHash: String,
        newExpiresAt: LocalDateTime
    ): Boolean {
        val session = sessions.find { it.refreshTokenHash == oldHash } ?: return false
        session.refreshTokenHash = newHash
        session.expiresAt = newExpiresAt
        return true
    }

    override suspend fun hasRefreshTokenHash(refreshTokenHash: String): Boolean {
        return sessions.any { it.refreshTokenHash == refreshTokenHash && it.expiresAt.isAfter(LocalDateTime.now()) }
    }

    override suspend fun revokeRefreshToken(refreshTokenHash: String) {
        sessions.removeIf { it.refreshTokenHash == refreshTokenHash }
    }

    override suspend fun revokeAllRefreshTokens(userId: String) {
        sessions.removeIf { it.userId == userId }
    }

    override suspend fun clearExpiredSessions() {
        sessions.removeIf { it.expiresAt.isBefore(LocalDateTime.now()) }
    }

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
        revokeAllRefreshTokens(userId)
    }
}
