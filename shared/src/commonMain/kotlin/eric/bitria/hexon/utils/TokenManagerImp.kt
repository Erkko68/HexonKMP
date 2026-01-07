package eric.bitria.hexon.utils

import eric.bitria.hexon.persistence.EncryptedData
import eric.bitria.hexon.persistence.SettingsManager

class TokenManagerImpl(
    private val settingsManager: SettingsManager,
    private val encryptedData: EncryptedData
) : TokenManager {
    
    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    override fun saveTokens(accessToken: String, refreshToken: String) {
        settingsManager.putString(ACCESS_TOKEN_KEY, accessToken)
        encryptedData.putString(REFRESH_TOKEN_KEY, refreshToken)
    }

    override fun getAccessToken(): String? = settingsManager.getString(ACCESS_TOKEN_KEY)
    override fun getRefreshToken(): String? = encryptedData.getString(REFRESH_TOKEN_KEY)

    override fun clearTokens() {
        settingsManager.remove(ACCESS_TOKEN_KEY)
        encryptedData.remove(REFRESH_TOKEN_KEY)
    }
}
