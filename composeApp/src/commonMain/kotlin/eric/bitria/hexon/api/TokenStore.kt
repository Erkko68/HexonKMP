package eric.bitria.hexon.api

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenStore(
    private val settings: Settings,
    private val persistRefreshToken: Boolean
) {
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    private val _refreshToken = MutableStateFlow(
        if (persistRefreshToken) settings.getStringOrNull(REFRESH_TOKEN_KEY) else null
    )
    val refreshToken: StateFlow<String?> = _refreshToken.asStateFlow()

    fun get(): Pair<String?, String?> {
        // On Web (persistRefreshToken = false), we return null for the refresh token
        // so Ktor doesn't send it in headers, allowing cookies to handle it.
        return _accessToken.value to (if (persistRefreshToken) _refreshToken.value else null)
    }

    fun save(accessToken: String?, refreshToken: String? = null) {
        _accessToken.value = accessToken
        if (persistRefreshToken && refreshToken != null) {
            _refreshToken.value = refreshToken
            settings.putString(REFRESH_TOKEN_KEY, refreshToken)
        }
    }

    fun clear() {
        _accessToken.value = null
        _refreshToken.value = null
        if (persistRefreshToken) {
            settings.remove(REFRESH_TOKEN_KEY)
        }
    }

    companion object {
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }
}
