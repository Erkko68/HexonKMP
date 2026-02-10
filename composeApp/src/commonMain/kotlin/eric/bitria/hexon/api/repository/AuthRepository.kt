package eric.bitria.hexon.api.repository

import eric.bitria.hexon.api.client.AuthClient
import eric.bitria.hexon.di.TokenStorage
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.dtos.auth.LogoutRequest
import eric.bitria.hexon.dtos.auth.LogoutResult
import eric.bitria.hexon.dtos.auth.RefreshRequest
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
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun register(email: String, username: String, password: String): ApiResult<RegisterResult> {
        return safeApiCall {
            authClient.register(RegisterRequest(email, username, password)).result
        }
    }

    override suspend fun refresh(): ApiResult<RefreshResult> {
        return safeApiCall {
            val response = authClient.refresh(RefreshRequest(tokenStorage.getRefresh() ?: ""))

            if (response.result == RefreshResult.SUCCESS) {
                response.accessToken?.let { tokenStorage.saveAccess(it) }
                response.refreshToken?.let { tokenStorage.saveRefresh(it) }
            } else {
                tokenStorage.clear()
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
