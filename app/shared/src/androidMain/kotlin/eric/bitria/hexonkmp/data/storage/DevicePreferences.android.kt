package eric.bitria.hexonkmp.data.storage

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

object AppContext {
    lateinit var app: Application
}

actual fun createDevicePreferences(): DevicePreferences =
    DataStoreDevicePreferences(AppContext.app.dataStore)

private val Application.dataStore by preferencesDataStore("device_prefs")

private class DataStoreDevicePreferences(
    private val store: DataStore<Preferences>,
) : DevicePreferences {
    private val playerIdKey = stringPreferencesKey("player_id")
    private val playerNameKey = stringPreferencesKey("player_name")
    private val tokenKey = stringPreferencesKey("auth_token")

    override suspend fun getPlayerId(): String? =
        store.data.map { it[playerIdKey] }.firstOrNull()

    override suspend fun setPlayerId(id: String) {
        store.edit { it[playerIdKey] = id }
    }

    override suspend fun getPlayerName(): String? =
        store.data.map { it[playerNameKey] }.firstOrNull()

    override suspend fun setPlayerName(name: String) {
        store.edit { it[playerNameKey] = name }
    }

    override suspend fun getToken(): String? =
        store.data.map { it[tokenKey] }.firstOrNull()

    override suspend fun setToken(token: String) {
        store.edit { it[tokenKey] = token }
    }
}
