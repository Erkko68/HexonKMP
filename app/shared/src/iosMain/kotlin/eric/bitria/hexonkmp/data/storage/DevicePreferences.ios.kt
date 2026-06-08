package eric.bitria.hexonkmp.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun createDevicePreferences(): DevicePreferences =
    DataStoreDevicePreferences(createIosDataStore())

@OptIn(ExperimentalForeignApi::class)
private fun createIosDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val docDir = NSFileManager.defaultManager
                .URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, false, null)
                ?.path ?: error("Cannot resolve iOS documents directory")
            "$docDir/device_prefs.preferences_pb".toPath()
        }
    )

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
