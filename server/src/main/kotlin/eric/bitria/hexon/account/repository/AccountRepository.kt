package eric.bitria.hexon.account.repository

interface AccountRepository {
    suspend fun deleteAccountById(id: String)
}