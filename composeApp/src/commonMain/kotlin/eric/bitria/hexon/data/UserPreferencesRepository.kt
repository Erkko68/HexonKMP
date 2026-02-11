package eric.bitria.hexon.data

import eric.bitria.hexon.model.SettingsUiState
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<SettingsUiState>

    suspend fun updateMasterVolume(volume: Float)
    suspend fun updateMusicVolume(volume: Float)
    suspend fun updateMirrorUI(enabled: Boolean)
    suspend fun updateMuteAll(enabled: Boolean)
    suspend fun updateDisableFriendRequests(enabled: Boolean)
    suspend fun updateDisableGameInvites(enabled: Boolean)
}

