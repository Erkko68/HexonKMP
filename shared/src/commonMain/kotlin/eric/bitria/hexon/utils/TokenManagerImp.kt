package eric.bitria.hexon.utils

class TokenManagerImpl : TokenManager {
    // In a real app, replace these with multiplatform-settings or DataStore
    private var _accessToken: String? = null
    private var _refreshToken: String? = null

    override fun saveTokens(accessToken: String, refreshToken: String) {
        _accessToken = accessToken
        _refreshToken = refreshToken
    }

    override fun getAccessToken(): String? = _accessToken
    override fun getRefreshToken(): String? = _refreshToken

    override fun clearTokens() {
        _accessToken = null
        _refreshToken = null
    }
}
