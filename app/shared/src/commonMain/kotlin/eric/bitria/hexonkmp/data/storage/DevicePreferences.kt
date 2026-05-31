package eric.bitria.hexonkmp.data.storage

interface DevicePreferences {
    suspend fun getOrCreatePlayerId(): String
}

expect fun createDevicePreferences(): DevicePreferences
