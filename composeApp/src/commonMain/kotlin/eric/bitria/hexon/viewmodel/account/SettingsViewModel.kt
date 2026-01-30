package eric.bitria.hexon.viewmodel.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import eric.bitria.hexon.api.repository.ApiResult
import eric.bitria.hexon.api.repository.AuthRepository
import eric.bitria.hexon.dtos.auth.LogoutResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val masterVolume: Float = 1.0f,
    val musicVolume: Float = 0.8f,
    val mirrorUI: Boolean = false,
    val muteAll: Boolean = false,
    val disableFriendRequests: Boolean = false,
    val disableGameInvites: Boolean = false
)

class SettingsViewModel(
    private val settingsManager: Settings,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    var state by mutableStateOf<ApiResult<LogoutResult>>(ApiResult.Idle)
        private set

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                masterVolume = settingsManager.getInt("master_volume", 100).toFloat() / 100f,
                musicVolume = settingsManager.getInt("music_volume", 80).toFloat() / 100f,
                mirrorUI = settingsManager.getBoolean("mirror_ui", false),
                muteAll = settingsManager.getBoolean("mute_all", false),
                disableFriendRequests = settingsManager.getBoolean("disable_friend_requests", false),
                disableGameInvites = settingsManager.getBoolean("disable_game_invites", false)
            )
        }
    }

    fun onMasterVolumeChanged(volume: Float) {
        _uiState.update { it.copy(masterVolume = volume) }
        settingsManager.putInt("master_volume", (volume * 100).toInt())
    }

    fun onMusicVolumeChanged(volume: Float) {
        _uiState.update { it.copy(musicVolume = volume) }
        settingsManager.putInt("music_volume", (volume * 100).toInt())
    }

    fun onMuteAllToggled(isMuted: Boolean) {
        _uiState.update { it.copy(muteAll = isMuted) }
        settingsManager.putBoolean("mute_all", isMuted)
    }

    fun onDisableFriendRequestsToggled(isDisabled: Boolean) {
        _uiState.update { it.copy(disableFriendRequests = isDisabled) }
        settingsManager.putBoolean("disable_friend_requests", isDisabled)
    }

    fun onDisableGameInvitesToggled(isDisabled: Boolean) {
        _uiState.update { it.copy(disableGameInvites = isDisabled) }
        settingsManager.putBoolean("disable_game_invites", isDisabled)
    }

    fun onMirrorUIToggled(isDisabled: Boolean) {
        _uiState.update { it.copy(mirrorUI = isDisabled) }
        settingsManager.putBoolean("mirror_ui", isDisabled)
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
