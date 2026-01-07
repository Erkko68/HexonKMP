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
        val verificationCode: String?,
        val resetCode: String? = null
    )

    override suspend fun usernameExists(username: String) = users.values.any { it.username == username }

    override suspend fun emailExists(email: String) = users.containsKey(email)

    override suspend fun isAccountVerified(email: String) = users[email]?.verified ?: false

    override suspend fun getVerificationCodeByEmail(email: String): String? {
        return users[email]?.verificationCode
    }

    override suspend fun markAccountAsVerified(email: String) {
        val user = users[email] ?: return
        users[email] = user.copy(verified = true, verificationCode = null)
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

    override suspend fun updateVerificationCode(email: String, verificationCode: String) {
        val user = users[email] ?: return
        users[email] = user.copy(verified = false, verificationCode = verificationCode)
    }

    override suspend fun getPasswordByEmail(email: String): String? {
        return users[email]?.passwordHash
    }

    override suspend fun updatePassword(email: String, passwordHash: String) {
        val user = users[email] ?: return
        users[email] = user.copy(passwordHash = passwordHash)
    }

    override suspend fun updateResetCode(email: String, resetCode: String) {
        val user = users[email] ?: return
        users[email] = user.copy(resetCode = resetCode)
    }

    override suspend fun getResetCodeByEmail(email: String): String? {
        return users[email]?.resetCode
    }

    override suspend fun clearResetCode(email: String) {
        val user = users[email] ?: return
        users[email] = user.copy(resetCode = null)
    }
}
