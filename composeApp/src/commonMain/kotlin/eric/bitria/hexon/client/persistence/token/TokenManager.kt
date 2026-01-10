package eric.bitria.hexon.client.persistence.token

import kotlinx.coroutines.flow.StateFlow

interface TokenManager {
    fun saveTokens(accessToken: String, refreshToken: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clearTokens()
}