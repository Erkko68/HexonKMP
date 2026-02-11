package eric.bitria.hexon.viewmodel.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.api.repository.ApiResult
import eric.bitria.hexon.api.repository.AuthRepository
import eric.bitria.hexon.data.UserPreferencesRepository
import eric.bitria.hexon.dtos.auth.LogoutResult
import eric.bitria.hexon.model.SettingsUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = userPreferencesRepository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState()
        )

    var state by mutableStateOf<ApiResult<LogoutResult>>(ApiResult.Idle)
        private set

    fun onMasterVolumeChanged(volume: Float) {
        viewModelScope.launch {
            userPreferencesRepository.updateMasterVolume(volume)
        }
    }

    fun onMusicVolumeChanged(volume: Float) {
        viewModelScope.launch {
            userPreferencesRepository.updateMusicVolume(volume)
        }
    }

    fun onMuteAllToggled(isMuted: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateMuteAll(isMuted)
        }
    }

    fun onDisableFriendRequestsToggled(isDisabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDisableFriendRequests(isDisabled)
        }
    }

    fun onDisableGameInvitesToggled(isDisabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDisableGameInvites(isDisabled)
        }
    }

    fun onMirrorUIToggled(isDisabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateMirrorUI(isDisabled)
        }
    }

    fun logout() {
        viewModelScope.launch {
            state = ApiResult.Loading

            when (val result = authRepository.logout(true)) {
                is ApiResult.Success -> {
                    state = when (result.data) {
                        LogoutResult.SUCCESS -> ApiResult.Success(LogoutResult.SUCCESS)
                        LogoutResult.INVALID_TOKEN -> ApiResult.Error("Invalid token.")
                        LogoutResult.UNKNOWN_ERROR -> ApiResult.Error("An unexpected error occurred.")
                    }
                }
                is ApiResult.NetworkError -> {
                    state = ApiResult.NetworkError
                }
                is ApiResult.Error -> {
                    state = ApiResult.Error(result.message ?: "Failed to delete account.")
                }
                else -> {}
            }
        }
    }
}
