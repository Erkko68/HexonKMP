package eric.bitria.hexon.client.repository

import eric.bitria.hexon.client.persistence.token.TokenManager
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KtorAuthClient(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) : AuthClient {

    override suspend fun register(request: RegisterRequest): RegisterResponse {
        return client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun login(request: LoginRequest): LoginResponse {
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<LoginResponse>()

        // Side Effect: Save tokens automatically on success
        if (response.result == LoginResult.SUCCESS) {
            tokenManager.saveTokens(response.accessToken!!, response.refreshToken!!)
        }

        return response
    }

    override suspend fun refresh(request: RefreshRequest): RefreshResponse {
        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<RefreshResponse>()

        if (response.result == RefreshResult.SUCCESS) {
            tokenManager.saveTokens(response.accessToken!!, response.refreshToken!!)
        }

        return response
    }

    override suspend fun autoLogin(): Boolean {
        val refreshToken = tokenManager.getRefreshToken() ?: return false
        return try {
            val response = refresh(RefreshRequest(refreshToken))
            response.result == RefreshResult.SUCCESS
        } catch (e: Exception) {
            false
        }
    }
}