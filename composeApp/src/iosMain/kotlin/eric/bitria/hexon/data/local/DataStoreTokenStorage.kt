package eric.bitria.hexon.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "DataStoreTokenStorage"

class DataStoreTokenStorage(
    private val dataStore: DataStore<Preferences>
) : TokenStorage {

    private val _accessToken = MutableStateFlow<String?>(null)
    override val accessToken = _accessToken.asStateFlow()

    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")

    // Scope for initialization
    private val storageScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        Logger.d(TAG) { "DataStoreTokenStorage init started" }
        // Initialize access token from DataStore on creation
        storageScope.launch {
            Logger.d(TAG) { "Starting DataStore initialization coroutine..." }
            try {
                Logger.d(TAG) { "Reading access token from DataStore..." }
                val storedAccessToken = dataStore.data.map { it[ACCESS_TOKEN_KEY] }.first()
                Logger.d(TAG) { "DataStore read completed. Token: ${if (storedAccessToken != null) "present" else "null"}" }
                _accessToken.value = storedAccessToken
                Logger.d(TAG) { "Access token StateFlow updated" }
            } catch (e: Exception) {
                Logger.e(TAG, e) { "DataStore read failed with exception: ${e.message}" }
                // If DataStore read fails, start with null (logged out state)
                _accessToken.value = null
            }
        }
        Logger.d(TAG) { "DataStoreTokenStorage init completed (async init launched)" }
    }

    override suspend fun saveAccess(token: String) {
        Logger.d(TAG) { "saveAccess() called" }
        _accessToken.value = token
        // Persist access token to DataStore so it survives app restarts
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = token
        }
        Logger.d(TAG) { "saveAccess() completed" }
    }

    override suspend fun saveRefresh(token: String) {
        Logger.d(TAG) { "saveRefresh() called" }
        dataStore.edit { preferences ->
            preferences[REFRESH_TOKEN_KEY] = token
        }
        Logger.d(TAG) { "saveRefresh() completed" }
    }

    override suspend fun getAccess(): String? {
        val token = _accessToken.value
        Logger.d(TAG) { "getAccess() returning: ${if (token != null) "present" else "null"}" }
        return token
    }

    override suspend fun getRefresh(): String? {
        Logger.d(TAG) { "getRefresh() called, reading from DataStore..." }
        val token = dataStore.data.map { it[REFRESH_TOKEN_KEY] }.first()
        Logger.d(TAG) { "getRefresh() returning: ${if (token != null) "present (${token.length} chars)" else "null"}" }
        return token
    }

    override suspend fun clear() {
        Logger.d(TAG) { "clear() called" }
        _accessToken.value = null
        dataStore.edit { preferences ->
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(ACCESS_TOKEN_KEY)
        }
        Logger.d(TAG) { "clear() completed" }
    }
}

