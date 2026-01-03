package eric.bitria.auth.mock

import eric.bitria.auth.register.RegisterRepository
import eric.bitria.hexon.dtos.auth.VerifyEmailResult

class MockRegisterRepository : RegisterRepository {

    // email -> (username, verified, verificationCode)
    private val users = mutableMapOf<String, Triple<String, Boolean, String>>()

    override fun usernameExists(username: String) = users.values.any { it.first == username }

    override fun emailExists(email: String) = users.containsKey(email)

    override fun isAccountVerified(email: String) = users[email]?.second ?: false

    override fun saveUser(email: String, username: String, password: String, verificationCode: String) {
        users[email] = Triple(username, false, verificationCode)
    }

    override fun getUserIdByEmail(email: String): String {
        return email
    }

    override fun verifyEmail(email: String, code: String): VerifyEmailResult {
        val user = users[email] ?: return VerifyEmailResult.INVALID_EMAIL
        val (username, verified, verificationCode) = user

        if (verified) return VerifyEmailResult.ACCOUNT_ALREADY_VERIFIED

        return if (code == verificationCode) {
            users[email] = Triple(username, true, verificationCode)
            VerifyEmailResult.SUCCESS
        } else {
            VerifyEmailResult.INVALID_VERIFICATION_CODE
        }
    }

    override fun updateVerificationCode(email: String, verificationCode: String) {
        val user = users[email] ?: return
        users[email] = Triple(user.first, false, verificationCode)
    }
}
