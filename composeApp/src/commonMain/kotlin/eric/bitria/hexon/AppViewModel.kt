package eric.bitria.hexon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import eric.bitria.hexon.data.local.TokenStorage
import eric.bitria.hexon.data.repository.ApiResult
import eric.bitria.hexon.data.repository.AuthRepository
import eric.bitria.hexon.dtos.auth.RefreshResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "AppViewModel"

enum class SessionState {
    LOADING, LOGGED_IN, LOGGED_OUT, NETWORK_ERROR
}

class AppViewModel(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _sessionState = MutableStateFlow(SessionState.LOADING)
    val sessionState = _sessionState.asStateFlow()

    init {
        Logger.d(TAG) { "AppViewModel init started" }

        // Run startup check first
        checkSession()

        // Then react to token changes (e.g. Logout from Settings screen)
        viewModelScope.launch {
            Logger.d(TAG) { "Starting token flow collection" }
            tokenStorage.accessToken.collect { token ->
                Logger.d(TAG) { "Token flow emitted: ${if (token != null) "token present" else "null"}, current state: ${_sessionState.value}" }
                // Only update state based on token changes if we're not in LOADING state
                // This prevents the flow collection from interfering with the initial refresh
                if (_sessionState.value != SessionState.LOADING) {
                    if (token != null) {
                        Logger.d(TAG) { "Token flow: setting state to LOGGED_IN" }
                        _sessionState.value = SessionState.LOGGED_IN
                    } else {
                        Logger.d(TAG) { "Token flow: setting state to LOGGED_OUT" }
                        _sessionState.value = SessionState.LOGGED_OUT
                    }
                } else {
                    Logger.d(TAG) { "Token flow: ignoring emission because state is LOADING" }
                }
            }
        }
    }

    fun checkSession() {
        viewModelScope.launch {
            Logger.d(TAG) { "checkSession started" }

            // Wait for the first emission from accessToken flow to ensure DataStore is initialized
            Logger.d(TAG) { "Waiting for DataStore initialization (first token emission)..." }
            val initialToken = tokenStorage.accessToken.first()
            Logger.d(TAG) { "DataStore initialized. Initial token: ${if (initialToken != null) "present" else "null"}" }

            Logger.d(TAG) { "Calling authRepository.refresh()..." }
            val result = authRepository.refresh()
            Logger.d(TAG) { "authRepository.refresh() returned: $result" }

            val state = when (result) {
                is ApiResult.Success -> {
                    Logger.d(TAG) { "Refresh success, result data: ${result.data}" }
                    if (result.data == RefreshResult.SUCCESS) SessionState.LOGGED_IN else SessionState.LOGGED_OUT
                }
                is ApiResult.NetworkError -> {
                    Logger.w(TAG) { "Refresh failed with NetworkError" }
                    SessionState.NETWORK_ERROR
                }
                is ApiResult.Error -> {
                    Logger.w(TAG) { "Refresh failed with Error: ${result.message}" }
                    SessionState.LOGGED_OUT
                }
                else -> {
                    Logger.w(TAG) { "Refresh returned unexpected result: $result" }
                    SessionState.LOGGED_OUT
                }
            }

            Logger.d(TAG) { "Setting session state to: $state" }
            _sessionState.value = state
            Logger.d(TAG) { "checkSession completed" }
        }
    }
}