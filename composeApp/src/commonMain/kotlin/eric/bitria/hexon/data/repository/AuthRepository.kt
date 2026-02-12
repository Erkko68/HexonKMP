package eric.bitria.hexon.data.repository

import co.touchlab.kermit.Logger
import eric.bitria.hexon.data.local.TokenStorage
import eric.bitria.hexon.data.remote.AuthClient
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.dtos.auth.LogoutRequest
import eric.bitria.hexon.dtos.auth.LogoutResult
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResult

private const val TAG = "AuthRepository"

interface AuthRepository {
    suspend fun register(email: String, username: String, password: String): ApiResult<RegisterResult>
    suspend fun refresh(): ApiResult<RefreshResult>
    suspend fun login(email: String, password: String): ApiResult<LoginResult>
    suspend fun logout(logoutAllDevices: Boolean): ApiResult<LogoutResult>
}

class AuthRepositoryImpl(
    private val authClient: AuthClient,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun register(email: String, username: String, password: String): ApiResult<RegisterResult> {
        return safeApiCall {
            authClient.register(RegisterRequest(email, username, password)).result
        }
    }

    override suspend fun refresh(): ApiResult<RefreshResult> {
        Logger.d(TAG) { "refresh() called" }
        return safeApiCall {
            Logger.d(TAG) { "Getting refresh token from storage..." }
            val refreshToken = tokenStorage.getRefresh()
            Logger.d(TAG) { "Refresh token from storage: ${if (refreshToken != null) "present (${refreshToken.length} chars)" else "null"}" }

            Logger.d(TAG) { "Calling authClient.refresh()..." }
            val response = authClient.refresh(RefreshRequest(refreshToken ?: ""))
            Logger.d(TAG) { "authClient.refresh() returned: result=${response.result}, hasAccessToken=${response.accessToken != null}" }

            if (response.result == RefreshResult.SUCCESS) {
                Logger.d(TAG) { "Refresh successful, saving tokens..." }
                response.accessToken?.let { tokenStorage.saveAccess(it) }
                response.refreshToken?.let { tokenStorage.saveRefresh(it) }
                Logger.d(TAG) { "Tokens saved successfully" }
            } else {
                Logger.d(TAG) { "Refresh failed, clearing tokens..." }
                tokenStorage.clear()
                Logger.d(TAG) { "Tokens cleared" }
            }
            response.result
        }
    }

    override suspend fun login(email: String, password: String): ApiResult<LoginResult> {
        return safeApiCall {
            val response = authClient.login(LoginRequest(email, password))
            if (response.result == LoginResult.SUCCESS) {
                response.accessToken?.let { tokenStorage.saveAccess(it) }
                response.refreshToken?.let { tokenStorage.saveRefresh(it) }
            }
            response.result
        }
    }

    override suspend fun logout(logoutAllDevices: Boolean): ApiResult<LogoutResult> {
        // 1. Tell server to delete the session (and invalidate cookie)
        val apiResult = safeApiCall {
            authClient.logout(LogoutRequest(tokenStorage.getRefresh() ?: "", logoutAllDevices)).result
        }

        // 2. Optimistic Logout: Clear local state immediately
        tokenStorage.clear()

        return apiResult
    }
}
