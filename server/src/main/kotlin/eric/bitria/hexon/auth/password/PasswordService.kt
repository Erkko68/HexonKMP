package eric.bitria.hexon.auth.password

interface PasswordService {
    suspend fun verifyPassword(userId: String, password: String): Boolean
    suspend fun updatePassword(userId: String, newPassword: String)
}