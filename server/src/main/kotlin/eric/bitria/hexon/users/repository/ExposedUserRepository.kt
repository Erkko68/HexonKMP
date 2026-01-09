package eric.bitria.hexon.users.repository

class ExposedUserRepository : UserRepository {
    override suspend fun getEmailByUserId(userId: String): String? {
        TODO("Not yet implemented")
    }

    override suspend fun getUserIdByEmail(email: String): String? {
        TODO("Not yet implemented")
    }

}