package eric.bitria.hexon.mock

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.auth.VerifyEmailResult

class MockAuthRepository : AuthRepository {

    // email -> (username, passwordHash, verified, verificationCode)
    private val users = mutableMapOf<String, UserData>()

    data class UserData(
        val username: String,
        val passwordHash: String,
        val verified: Boolean,
        val verificationCode: String
    )

    override suspend fun usernameExists(username: String) = users.values.any { it.username == username }

    override suspend fun emailExists(email: String) = users.containsKey(email)

    override suspend fun isAccountVerified(email: String) = users[email]?.verified ?: false

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

    override suspend fun verifyEmail(email: String, code: String): VerifyEmailResult {
        val user = users[email] ?: return VerifyEmailResult.INVALID_EMAIL

        if (user.verified) return VerifyEmailResult.ACCOUNT_ALREADY_VERIFIED

        return if (code == user.verificationCode) {
            users[email] = user.copy(verified = true)
            VerifyEmailResult.SUCCESS
        } else {
            VerifyEmailResult.INVALID_VERIFICATION_CODE
        }
    }

    override suspend fun updateVerificationCode(email: String, verificationCode: String) {
        val user = users[email] ?: return
        users[email] = user.copy(verified = false, verificationCode = verificationCode)
    }

    override suspend fun getPasswordByEmail(email: String): String? {
        return users[email]?.passwordHash
    }
}
