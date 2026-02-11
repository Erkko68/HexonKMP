package eric.bitria.hexon.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreTokenStorage(
    private val dataStore: DataStore<Preferences>
) : TokenStorage {

    private val _accessToken = MutableStateFlow<String?>(null)
    override val accessToken = _accessToken.asStateFlow()

    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")

    override suspend fun saveAccess(token: String) {
        _accessToken.value = token
    }

    override suspend fun saveRefresh(token: String) {
        dataStore.edit { preferences ->
            preferences[REFRESH_TOKEN_KEY] = token
        }
    }

    override suspend fun getAccess(): String? = _accessToken.value

    override suspend fun getRefresh(): String? {
        return dataStore.data.map { it[REFRESH_TOKEN_KEY] }.first()
    }

    override suspend fun clear() {
        _accessToken.value = null
        dataStore.edit { preferences ->
            preferences.remove(REFRESH_TOKEN_KEY)
        }
    }
}

