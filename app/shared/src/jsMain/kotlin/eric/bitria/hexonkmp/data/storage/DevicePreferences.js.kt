package eric.bitria.hexonkmp.data.storage

import kotlinx.browser.localStorage

private const val ID_KEY = "player_id"
private const val NAME_KEY = "player_name"
private const val TOKEN_KEY = "auth_token"

actual fun createDevicePreferences(): DevicePreferences = object : DevicePreferences {
    override suspend fun getPlayerId(): String? = localStorage.getItem(ID_KEY)

    override suspend fun setPlayerId(id: String) = localStorage.setItem(ID_KEY, id)

    override suspend fun getPlayerName(): String? = localStorage.getItem(NAME_KEY)

    override suspend fun setPlayerName(name: String) = localStorage.setItem(NAME_KEY, name)

    override suspend fun getToken(): String? = localStorage.getItem(TOKEN_KEY)

    override suspend fun setToken(token: String) = localStorage.setItem(TOKEN_KEY, token)
}
