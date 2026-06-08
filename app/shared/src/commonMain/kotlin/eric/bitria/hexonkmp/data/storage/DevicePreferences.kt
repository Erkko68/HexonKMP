package eric.bitria.hexonkmp.data.storage

// Local, per-device storage of the player's identity. The playerId is now issued
// by the server (via /register) and persisted here for reuse across sessions; the
// name is the display name the player chose. Both are null until first set.
interface DevicePreferences {
    suspend fun getPlayerId(): String?
    suspend fun setPlayerId(id: String)
    suspend fun getPlayerName(): String?
    suspend fun setPlayerName(name: String)
}

expect fun createDevicePreferences(): DevicePreferences
