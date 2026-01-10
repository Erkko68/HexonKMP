package eric.bitria.hexon.client.auth

import eric.bitria.hexon.client.AuthClient
import eric.bitria.hexon.client.persistence.AccountManager
import eric.bitria.hexon.client.persistence.SettingsManager
import eric.bitria.hexon.client.persistence.token.TokenManager
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class SessionState {
    LOADING, // Initial state
    LOGGED_IN,
    LOGGED_OUT
}

object SessionManager : KoinComponent {
    private val tokenManager: TokenManager by inject()
    private val client: AuthClient by inject()
    private val accountManager: AccountManager by inject()
    private val settingsManager: SettingsManager by inject()


    // Start with LOADING
    private val _sessionState = MutableStateFlow(SessionState.LOADING)
    val sessionState = _sessionState.asStateFlow()

    // 2. Call this once when App starts
    suspend fun initSession() {
        // If we are already loaded prevent another load
        if (_sessionState.value != SessionState.LOADING) {
            return
        }

        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken == null) {
            _sessionState.value = SessionState.LOGGED_OUT
            return
        }

        try {
            val response = client.refresh(RefreshRequest(refreshToken))
            if (response.result == RefreshResult.SUCCESS) {
                _sessionState.value = SessionState.LOGGED_IN
            } else {
                logout()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logout()
        }
    }

    fun login() {
        // Called by LoginViewModel on success
        _sessionState.value = SessionState.LOGGED_IN
    }

    fun logout() {
        accountManager.clear()
        settingsManager.clear()
        tokenManager.clearTokens()
        _sessionState.value = SessionState.LOGGED_OUT
    }
}