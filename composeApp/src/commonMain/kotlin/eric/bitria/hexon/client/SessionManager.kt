package eric.bitria.hexon.client

import eric.bitria.hexon.client.persistence.AccountManager
import eric.bitria.hexon.client.persistence.SettingsManager
import eric.bitria.hexon.client.persistence.EncryptedData
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SessionState {
    LOADING,
    LOGGED_IN,
    LOGGED_OUT
}

class SessionManager(
    private val authClientProvider: () -> AuthClient, // Break circular dependency
    private val accountManager: AccountManager,
    private val settingsManager: SettingsManager,
    private val encryptedData: EncryptedData
) {
    private val _sessionState = MutableStateFlow(SessionState.LOADING)
    val sessionState = _sessionState.asStateFlow()

    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        settingsManager.putString(ACCESS_TOKEN_KEY, accessToken)
        encryptedData.putString(REFRESH_TOKEN_KEY, refreshToken)
    }

    fun getAccessToken(): String? = settingsManager.getString(ACCESS_TOKEN_KEY)
    fun getRefreshToken(): String? = encryptedData.getString(REFRESH_TOKEN_KEY)

    suspend fun initSession() {
        if (_sessionState.value != SessionState.LOADING) return

        val refreshToken = getRefreshToken()
        if (refreshToken == null) {
            _sessionState.value = SessionState.LOGGED_OUT
            return
        }

        try {
            // Access the client lazily
            val response = authClientProvider().refresh(RefreshRequest(refreshToken))
            if (response.result == RefreshResult.SUCCESS) {
                _sessionState.value = SessionState.LOGGED_IN
            } else {
                logout()
            }
        } catch (e: Exception) {
            logout()
        }
    }

    fun login() {
        _sessionState.value = SessionState.LOGGED_IN
    }

    fun logout() {
        accountManager.clear()
        settingsManager.clear()
        settingsManager.remove(ACCESS_TOKEN_KEY)
        encryptedData.remove(REFRESH_TOKEN_KEY)
        _sessionState.value = SessionState.LOGGED_OUT
    }
}
