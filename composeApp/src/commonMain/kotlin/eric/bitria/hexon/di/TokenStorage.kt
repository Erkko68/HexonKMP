package eric.bitria.hexon.di

import kotlinx.coroutines.flow.Flow

interface TokenStorage {
    val accessToken: Flow<String?>

    suspend fun saveAccess(token: String)
    suspend fun saveRefresh(token: String)
    suspend fun getAccess(): String?
    suspend fun getRefresh(): String?
    suspend fun clear()
}