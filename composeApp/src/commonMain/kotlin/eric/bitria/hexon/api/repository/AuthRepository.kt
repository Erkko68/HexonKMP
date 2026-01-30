package eric.bitria.hexon.api.repository

import eric.bitria.hexon.api.TokenStore
import eric.bitria.hexon.api.client.AuthClient
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.dtos.auth.LogoutRequest
import eric.bitria.hexon.dtos.auth.LogoutResult
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResult

interface AuthRepository {
    suspend fun register(email: String, username: String, password: String): ApiResult<RegisterResult>
    suspend fun refresh(): ApiResult<RefreshResult>
    suspend fun login(email: String, password: String): ApiResult<LoginResult>
    suspend fun logout(logoutAllDevices: Boolean): ApiResult<LogoutResult>
}

class AuthRepositoryImpl(
    private val authClient: AuthClient,
    private val tokenStore: TokenStore
) : AuthRepository {

    override suspend fun register(email: String, username: String, password: String): ApiResult<RegisterResult> {
        return safeApiCall {
            authClient.register(RegisterRequest(email, username, password)).result
        }
    }

    override suspend fun refresh(): ApiResult<RefreshResult> {
        // Check if we have a cookie on disk before making a network call.
        if (!tokenStore.hasSessionCookie()) {
            return ApiResult.Success(RefreshResult.INVALID_TOKEN)
        }

        return safeApiCall {
            val response = authClient.refresh()

            if (response.result == RefreshResult.SUCCESS) {
                tokenStore.save(response.accessToken!!)
            } else {
                tokenStore.clear()
            }
            response.result
        }
    }

    override suspend fun login(email: String, password: String): ApiResult<LoginResult> {
        return safeApiCall {
            val response = authClient.login(LoginRequest(email, password))
            if (response.result == LoginResult.SUCCESS) {
                // Repository responsibility: Update the local store
                tokenStore.save(response.accessToken!!)
            }
            response.result
        }
    }

    override suspend fun logout(logoutAllDevices: Boolean): ApiResult<LogoutResult> {
        // 1. Optimistic Logout: Clear local state immediately
        tokenStore.clear()

        // 2. Tell server to delete the session (and invalidate cookie)
        return safeApiCall {
            authClient.logout(LogoutRequest(logoutAllDevices)).result
        }
    }
}
