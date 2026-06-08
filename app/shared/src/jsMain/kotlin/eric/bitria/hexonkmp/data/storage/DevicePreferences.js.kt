package eric.bitria.hexonkmp.data.storage

import kotlinx.browser.localStorage

private const val ID_KEY = "player_id"
private const val NAME_KEY = "player_name"

actual fun createDevicePreferences(): DevicePreferences = object : DevicePreferences {
    override suspend fun getPlayerId(): String? = localStorage.getItem(ID_KEY)

    override suspend fun setPlayerId(id: String) = localStorage.setItem(ID_KEY, id)

    override suspend fun getPlayerName(): String? = localStorage.getItem(NAME_KEY)

    override suspend fun setPlayerName(name: String) = localStorage.setItem(NAME_KEY, name)
}
