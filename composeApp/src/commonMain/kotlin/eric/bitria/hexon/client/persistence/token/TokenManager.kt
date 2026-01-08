package eric.bitria.hexon.client.persistence.token

interface TokenManager {
    fun saveTokens(accessToken: String, refreshToken: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clearTokens()
}