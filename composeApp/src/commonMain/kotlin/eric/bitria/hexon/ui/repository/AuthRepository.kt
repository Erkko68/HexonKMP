package eric.bitria.hexon.ui.repository

import eric.bitria.hexon.api.client.AuthClient
import eric.bitria.hexon.api.client.SessionManager
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResult

interface AuthRepository {
    suspend fun register(email: String, username: String, password: String): ApiResult<RegisterResult>
    suspend fun login(email: String, password: String): ApiResult<LoginResult>
}

class AuthRepositoryImpl(
    private val authClient: AuthClient,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun register(email: String, username: String, password: String): ApiResult<RegisterResult> {
        return safeApiCall {
            authClient.register(RegisterRequest(email, username, password)).result
        }
    }

    override suspend fun login(email: String, password: String): ApiResult<LoginResult> {
        return safeApiCall {
            val response = authClient.login(LoginRequest(email, password))
            if (response.result == LoginResult.SUCCESS) {
                sessionManager.saveTokens(response.accessToken!!, response.refreshToken!!)
            }
            response.result
        }
    }
}
