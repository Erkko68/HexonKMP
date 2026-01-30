package eric.bitria.hexon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.api.TokenStore
import eric.bitria.hexon.api.repository.AuthRepository
import eric.bitria.hexon.api.repository.ApiResult
import eric.bitria.hexon.dtos.auth.RefreshResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SessionState {
    LOADING, LOGGED_IN, LOGGED_OUT, NETWORK_ERROR
}

class AppViewModel(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _sessionState = MutableStateFlow(SessionState.LOADING)
    val sessionState = _sessionState.asStateFlow()

    init {
        // 1. React to changes (e.g. Logout from Settings screen)
        viewModelScope.launch {
            tokenStore.accessToken.collect { token ->
                if (token != null) {
                    _sessionState.value = SessionState.LOGGED_IN
                } else if (_sessionState.value != SessionState.LOADING) {
                    // Only switch to OUT if we aren't currently loading/retrying
                    _sessionState.value = SessionState.LOGGED_OUT
                }
            }
        }

        // 2. Run Startup Check
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _sessionState.value = SessionState.LOADING

            if (!tokenStore.hasSessionCookie()) {
                _sessionState.value = SessionState.LOGGED_OUT
                return@launch
            }

            // Standard Repository Call
            when (val result = authRepository.refresh()) {
                is ApiResult.Success -> {
                    if (result.data == RefreshResult.SUCCESS) {
                        // Success! TokenStore is updated. Observer (1) sets LOGGED_IN.
                    } else {
                        // Token expired or invalid
                        _sessionState.value = SessionState.LOGGED_OUT
                    }
                }
                is ApiResult.NetworkError -> {
                    // Optional: Show "Retry" screen
                    _sessionState.value = SessionState.NETWORK_ERROR
                }
                is ApiResult.Error -> {
                    _sessionState.value = SessionState.LOGGED_OUT
                }
                else -> { _sessionState.value = SessionState.LOGGED_OUT }
            }
        }
    }
}