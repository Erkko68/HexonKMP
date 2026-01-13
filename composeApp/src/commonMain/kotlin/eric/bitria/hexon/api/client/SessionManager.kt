package eric.bitria.hexon.api.client

import com.russhwolf.settings.Settings
import eric.bitria.hexon.api.persistence.EncryptedData
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
    private val settings: Settings,
    private val encryptedData: EncryptedData
) {
    private val _sessionState = MutableStateFlow(SessionState.LOADING)
    val sessionState = _sessionState.asStateFlow()

    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        settings.putString(ACCESS_TOKEN_KEY, accessToken)
        encryptedData.putString(REFRESH_TOKEN_KEY, refreshToken)
    }

    fun getAccessToken(): String = settings.getString(ACCESS_TOKEN_KEY,"")
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
        settings.clear()
        settings.remove(ACCESS_TOKEN_KEY)
        encryptedData.remove(REFRESH_TOKEN_KEY)
        _sessionState.value = SessionState.LOGGED_OUT
    }
}
