package eric.bitria.hexonkmp.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import java.util.UUID

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

    override suspend fun getOrCreatePlayerId(): String {
        val existing = store.data.map { it[playerIdKey] }.firstOrNull()
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        store.edit { it[playerIdKey] = newId }
        return newId
    }
}
