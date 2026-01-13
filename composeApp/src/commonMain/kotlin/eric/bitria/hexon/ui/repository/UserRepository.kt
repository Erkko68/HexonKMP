package eric.bitria.hexon.ui.repository

import eric.bitria.hexon.api.client.UserClient
import eric.bitria.hexon.dtos.profile.UserProfileResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UserRepository(private val userClient: UserClient) {
    private val _profile = MutableStateFlow<UserProfileResponse?>(null)
    val profile = _profile.asStateFlow()

    private val mutex = Mutex()

    suspend fun getProfile(forceRefresh: Boolean = false): UserProfileResponse {
        mutex.withLock {
            val currentProfile = _profile.value
            if (currentProfile != null && !forceRefresh) {
                return currentProfile
            }

            val response = userClient.getMe()
            _profile.value = response
            return response
        }
    }

    fun clearCache() {
        _profile.value = null
    }
}