package eric.bitria.hexon.di

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MobileTokenStorage(private val settings: Settings) : TokenStorage {
    private val _accessToken = MutableStateFlow<String?>(null)
    override val accessToken = _accessToken.asStateFlow()

    private val REFRESH_TOKEN_KEY = "refresh_token"

    override suspend fun saveAccess(token: String) {
        _accessToken.value = token
    }

    override suspend fun saveRefresh(token: String) {
        settings.putString(REFRESH_TOKEN_KEY, token)
    }

    override suspend fun getAccess(): String? = _accessToken.value

    override suspend fun getRefresh(): String? = settings.getStringOrNull(REFRESH_TOKEN_KEY)

    override suspend fun clear() {
        _accessToken.value = null
        settings.remove(REFRESH_TOKEN_KEY)
    }
}