package eric.bitria.hexon.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebTokenStorage : TokenStorage {
    private val _accessToken = MutableStateFlow<String?>(null)
    override val accessToken = _accessToken.asStateFlow()

    override suspend fun saveAccess(token: String) {
        _accessToken.value = token
    }

    override suspend fun saveRefresh(token: String) {
        // No-op for web
    }

    override suspend fun getAccess(): String? {
        return _accessToken.value
    }

    override suspend fun getRefresh(): String? {
        // No-op for web
        return null
    }

    override suspend fun clear() {
        _accessToken.value = null
    }
}