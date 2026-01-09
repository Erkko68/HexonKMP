package eric.bitria.hexon.users.repository

interface UserRepository {
    suspend fun getEmailByUserId(userId: String): String?
    suspend fun getUserIdByEmail(email: String): String?
}