package eric.bitria.hexon.api

import com.russhwolf.settings.Settings
import eric.bitria.hexon.api.client.AuthClient
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
    private val authClientProvider: () -> AuthClient,
    private val settings: Settings,
) {
    private val _sessionState = MutableStateFlow(SessionState.LOADING)
    val sessionState = _sessionState.asStateFlow()


    fun saveTokens(accessToken: String, refreshToken: String) {
        settings.putString("access_token", accessToken)
        settings.putString("refresh_token", refreshToken)
    }

    fun getAccessToken(): String = settings.getString("access_token","")
    fun getRefreshToken(): String = settings.getString("refresh_token","")

    suspend fun initSession() {
        if (_sessionState.value != SessionState.LOADING) return

        val refreshToken = getRefreshToken()
        if (refreshToken.isEmpty()) {
            _sessionState.value = SessionState.LOGGED_OUT
            return
        }

        try {
            // Access the client lazily
            val response = authClientProvider().refresh(RefreshRequest(refreshToken))
            if (response.result == RefreshResult.SUCCESS) {
                _sessionState.value = SessionState.LOGGED_IN
                saveTokens(response.accessToken!!, response.refreshToken!!)
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
        settings.remove("access_token")
        settings.remove("refresh_token")
        _sessionState.value = SessionState.LOGGED_OUT
    }
}
