package eric.bitria.hexon.client.persistence.token

import kotlinx.coroutines.flow.StateFlow

interface TokenManager {
    val isSessionValid: StateFlow<Boolean>
    fun saveTokens(accessToken: String, refreshToken: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clearTokens()
}