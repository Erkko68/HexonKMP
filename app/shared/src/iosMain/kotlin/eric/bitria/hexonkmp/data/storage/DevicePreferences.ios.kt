package eric.bitria.hexonkmp.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import kotlin.random.Random

actual fun createDevicePreferences(): DevicePreferences =
    DataStoreDevicePreferences(createIosDataStore())

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

    override suspend fun getOrCreatePlayerId(): String {
        val existing = store.data.map { it[playerIdKey] }.firstOrNull()
        if (existing != null) return existing
        val newId = randomUuid()
        store.edit { it[playerIdKey] = newId }
        return newId
    }
}

private fun randomUuid(): String {
    val b = Random.nextBytes(16)
    b[6] = ((b[6].toInt() and 0x0f) or 0x40).toByte()
    b[8] = ((b[8].toInt() and 0x3f) or 0x80).toByte()
    return b.toHexString().let {
        "${it.substring(0,8)}-${it.substring(8,12)}-${it.substring(12,16)}-${it.substring(16,20)}-${it.substring(20)}"
    }
}

private fun ByteArray.toHexString() = joinToString("") {
    it.toInt().and(0xff).toString(16).padStart(2, '0')
}
