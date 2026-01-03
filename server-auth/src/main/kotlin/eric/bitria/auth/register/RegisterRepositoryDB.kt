package eric.bitria.auth.register

import eric.bitria.hexon.dtos.auth.VerifyEmailResult

class RegisterRepositoryDB : RegisterRepository {
    override fun usernameExists(username: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun emailExists(email: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun isAccountVerified(email: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun saveUser(
        email: String,
        username: String,
        password: String,
        verificationCode: String
    ) {
        TODO("Not yet implemented")
    }

    override fun getUserIdByEmail(email: String): String {
        TODO("Not yet implemented")
    }

    override fun verifyEmail(
        email: String,
        code: String
    ): VerifyEmailResult {
        TODO("Not yet implemented")
    }

    override fun updateVerificationCode(email: String, verificationCode: String) {
        TODO("Not yet implemented")
    }
}