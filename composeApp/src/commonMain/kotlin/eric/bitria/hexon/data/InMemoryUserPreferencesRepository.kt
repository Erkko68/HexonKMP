package eric.bitria.hexon.data

import eric.bitria.hexon.model.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryUserPreferencesRepository : UserPreferencesRepository {

    private val _preferences = MutableStateFlow(SettingsUiState())
    override val preferences = _preferences.asStateFlow()

    override suspend fun updateMasterVolume(volume: Float) {
        _preferences.update { it.copy(masterVolume = volume) }
    }

    override suspend fun updateMusicVolume(volume: Float) {
        _preferences.update { it.copy(musicVolume = volume) }
    }

    override suspend fun updateMirrorUI(enabled: Boolean) {
        _preferences.update { it.copy(mirrorUI = enabled) }
    }

    override suspend fun updateMuteAll(enabled: Boolean) {
        _preferences.update { it.copy(muteAll = enabled) }
    }

    override suspend fun updateDisableFriendRequests(enabled: Boolean) {
        _preferences.update { it.copy(disableFriendRequests = enabled) }
    }

    override suspend fun updateDisableGameInvites(enabled: Boolean) {
        _preferences.update { it.copy(disableGameInvites = enabled) }
    }
}

