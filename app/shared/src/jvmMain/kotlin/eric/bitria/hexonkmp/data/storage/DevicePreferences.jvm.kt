package eric.bitria.hexonkmp.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

actual fun createDevicePreferences(): DevicePreferences =
    DataStoreDevicePreferences(createJvmDataStore())

private fun createJvmDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val dir = System.getProperty("user.home") + "/.hexon"
            java.io.File(dir).mkdirs()
            "$dir/device_prefs.preferences_pb".toPath()
        }
    )

private class DataStoreDevicePreferences(
    private val store: DataStore<Preferences>,
) : DevicePreferences {
    private val playerIdKey = stringPreferencesKey("player_id")
    private val playerNameKey = stringPreferencesKey("player_name")

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
}
