package eric.bitria.hexon.auth.mock

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.auth.repository.AuthRepository

class MockAuthRepository : AuthRepository {

    // email -> UserData
    private val users = mutableMapOf<String, UserData>()

    data class UserData(
        val username: String,
        val passwordHash: String,
        val verified: Boolean,
        val code: String? = null
    )

    override suspend fun usernameExists(username: String) = users.values.any { it.username == username }

    override suspend fun emailExists(email: String) = users.containsKey(email)

    override suspend fun isAccountVerified(email: String) = users[email]?.verified ?: false

    override suspend fun getVerificationCodeByEmail(email: String): String? {
        return users[email]?.code
    }

    override suspend fun markAccountAsVerified(email: String) {
        val user = users[email] ?: return
        users[email] = user.copy(verified = true, code = null)
    }

    override suspend fun saveOrUpdateUnverifiedUser(
        email: String,
        username: String,
        password: String,
        verificationCode: String
    ) {
        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val existingUser = users[email]
        if (existingUser == null || !existingUser.verified) {
            users[email] = UserData(username, passwordHash, false, verificationCode)
        }
    }

    override suspend fun getUserIdByEmail(email: String): String {
        return email
    }

    override suspend fun getEmailByUsername(username: String): String? {
        return users.entries.firstOrNull { it.value.username == username }?.key
    }

    override suspend fun getPasswordByEmail(email: String): String? {
        return users[email]?.passwordHash
    }

    override suspend fun updatePassword(email: String, passwordHash: String) {
        val user = users[email] ?: return
        users[email] = user.copy(passwordHash = passwordHash)
    }

    override suspend fun updateUserCodeByEmail(email: String, resetCode: String) {
        val user = users[email] ?: return
        users[email] = user.copy(code = resetCode)
    }

    override suspend fun getUserCodeByEmail(email: String): String? {
        return users[email]?.code
    }

    override suspend fun clearUserCode(email: String) {
        val user = users[email] ?: return
        users[email] = user.copy(code = null)
    }

    override suspend fun updateRefreshTokenHash(userId: String, hash: String) {
        users[userId] = users[userId]?.copy(passwordHash = hash) ?: return
    }

    override suspend fun getRefreshTokenHash(userId: String): String? {
        return users[userId]?.passwordHash
    }
}
