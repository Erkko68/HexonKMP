package eric.bitria.hexon.viewmodel

import androidx.lifecycle.ViewModel
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


class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onMasterVolumeChanged(volume: Float) {
        _uiState.update { it.copy(masterVolume = volume) }
        // TODO: Add logic to update the game's actual master volume
    }

    fun onMusicVolumeChanged(volume: Float) {
        _uiState.update { it.copy(musicVolume = volume) }
        // TODO: Add logic to update the game's actual music volume
    }

    fun onMuteAllToggled(isMuted: Boolean) {
        _uiState.update { it.copy(muteAll = isMuted) }
        // TODO: Add logic to mute/unmute all game sounds
    }

    fun onDisableFriendRequestsToggled(isDisabled: Boolean) {
        _uiState.update { it.copy(disableFriendRequests = isDisabled) }
        // TODO: Add logic to update user's privacy settings
    }

    fun onDisableGameInvitesToggled(isDisabled: Boolean) {
        _uiState.update { it.copy(disableGameInvites = isDisabled) }
        // TODO: Add logic to update user's privacy settings
    }

    fun onMirrorUIToggled(isDisabled: Boolean) {
        _uiState.update { it.copy(mirrorUI = isDisabled) }
        // TODO: Add logic to update user's privacy settings
    }

    fun onLogOutClicked() {
        // TODO: Add logic to log the user out and navigate to login screen
        println("Log Out Clicked")
    }

    fun onDeleteAccountClicked() {
        // TODO: Add logic to show a confirmation dialog, then delete the account
        println("Delete Account Clicked")
    }
}