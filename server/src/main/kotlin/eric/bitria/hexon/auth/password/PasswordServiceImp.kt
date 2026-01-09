package eric.bitria.hexon.auth.password

import at.favre.lib.crypto.bcrypt.BCrypt
import eric.bitria.hexon.auth.repository.AuthRepository

class PasswordServiceImp(
    private val repository: AuthRepository
) : PasswordService {
    override suspend fun verifyPassword(userId: String, password: String): Boolean {
        val hashedPassword = repository.getPasswordByUserId(userId)
            ?: return false

        val result = BCrypt.verifyer().verify(password.toCharArray(), hashedPassword)
        return result.verified
    }

    override suspend fun updatePassword(userId: String, newPassword: String) {
        val hashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray())
        repository.updatePasswordByUserId(userId, hashedPassword)
    }
}