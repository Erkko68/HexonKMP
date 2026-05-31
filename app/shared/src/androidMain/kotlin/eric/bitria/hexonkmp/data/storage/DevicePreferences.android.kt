package eric.bitria.hexonkmp.data.storage

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID

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

    override suspend fun getOrCreatePlayerId(): String {
        val existing = store.data.map { it[playerIdKey] }.firstOrNull()
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        store.edit { it[playerIdKey] = newId }
        return newId
    }
}
