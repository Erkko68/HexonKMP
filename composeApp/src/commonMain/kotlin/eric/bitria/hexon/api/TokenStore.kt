package eric.bitria.hexon.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenStore(
    private val persistentCookieStorage: PersistentCookieStorage
) {
    // RAM: The Access Token
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken = _accessToken.asStateFlow()

    fun get(): String? = _accessToken.value

    fun save(token: String) {
        _accessToken.value = token
    }

    suspend fun clear() {
        _accessToken.value = null
        persistentCookieStorage.clear() // Wipes the cookie from disk
    }

    suspend fun hasSessionCookie() : Boolean = persistentCookieStorage.hasSessionCookie()
}