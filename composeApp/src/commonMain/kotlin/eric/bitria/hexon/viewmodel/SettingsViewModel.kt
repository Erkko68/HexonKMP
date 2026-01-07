package eric.bitria.hexon.viewmodel

import androidx.lifecycle.ViewModel
import eric.bitria.hexon.persistence.SettingsManager
import eric.bitria.hexon.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val masterVolume: Float = 1.0f,
    val musicVolume: Float = 0.8f,
    val mirrorUI: Boolean = false,
    val muteAll: Boolean = false,
    val disableFriendRequests: Boolean = false,
    val disableGameInvites: Boolean = false
)

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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

    fun onLogOutClicked() {
        tokenManager.clearTokens()
    }

    fun onDeleteAccountClicked() {
        // Handle account deletion logic
    }
}
