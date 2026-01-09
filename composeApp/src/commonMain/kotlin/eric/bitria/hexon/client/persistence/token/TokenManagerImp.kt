package eric.bitria.hexon.client.persistence.token

import eric.bitria.hexon.client.persistence.EncryptedData
import eric.bitria.hexon.client.persistence.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenManagerImpl(
    private val settingsManager: SettingsManager,
    private val encryptedData: EncryptedData
) : TokenManager {
    
    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    private val _isSessionValid = MutableStateFlow(getAccessToken() != null && getRefreshToken() != null)
    override val isSessionValid: StateFlow<Boolean> = _isSessionValid.asStateFlow()

    override fun saveTokens(accessToken: String, refreshToken: String) {
        settingsManager.putString(ACCESS_TOKEN_KEY, accessToken)
        encryptedData.putString(REFRESH_TOKEN_KEY, refreshToken)
        _isSessionValid.value = true
    }

    override fun getAccessToken(): String? = settingsManager.getString(ACCESS_TOKEN_KEY)
    override fun getRefreshToken(): String? = encryptedData.getString(REFRESH_TOKEN_KEY)

    override fun clearTokens() {
        settingsManager.remove(ACCESS_TOKEN_KEY)
        encryptedData.remove(REFRESH_TOKEN_KEY)
        _isSessionValid.value = false
    }
}
