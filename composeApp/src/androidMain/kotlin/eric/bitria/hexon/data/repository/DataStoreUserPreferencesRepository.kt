package eric.bitria.hexon.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import eric.bitria.hexon.data.model.SettingsUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException

class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val MASTER_VOLUME = intPreferencesKey("master_volume")
        val MUSIC_VOLUME = intPreferencesKey("music_volume")
        val MIRROR_UI = booleanPreferencesKey("mirror_ui")
        val MUTE_ALL = booleanPreferencesKey("mute_all")
        val DISABLE_FRIEND_REQUESTS = booleanPreferencesKey("disable_friend_requests")
        val DISABLE_GAME_INVITES = booleanPreferencesKey("disable_game_invites")
    }

    override val preferences: Flow<SettingsUiState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            SettingsUiState(
                masterVolume = (preferences[PreferencesKeys.MASTER_VOLUME] ?: 100).toFloat() / 100f,
                musicVolume = (preferences[PreferencesKeys.MUSIC_VOLUME] ?: 80).toFloat() / 100f,
                mirrorUI = preferences[PreferencesKeys.MIRROR_UI] ?: false,
                muteAll = preferences[PreferencesKeys.MUTE_ALL] ?: false,
                disableFriendRequests = preferences[PreferencesKeys.DISABLE_FRIEND_REQUESTS] ?: false,
                disableGameInvites = preferences[PreferencesKeys.DISABLE_GAME_INVITES] ?: false
            )
        }

    override suspend fun updateMasterVolume(volume: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MASTER_VOLUME] = (volume * 100).toInt()
        }
    }

    override suspend fun updateMusicVolume(volume: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MUSIC_VOLUME] = (volume * 100).toInt()
        }
    }

    override suspend fun updateMirrorUI(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MIRROR_UI] = enabled
        }
    }

    override suspend fun updateMuteAll(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MUTE_ALL] = enabled
        }
    }

    override suspend fun updateDisableFriendRequests(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISABLE_FRIEND_REQUESTS] = enabled
        }
    }

    override suspend fun updateDisableGameInvites(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISABLE_GAME_INVITES] = enabled
        }
    }
}

