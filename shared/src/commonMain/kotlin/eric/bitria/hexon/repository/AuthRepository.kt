package eric.bitria.hexon.repository

import eric.bitria.hexon.dtos.auth.*
import eric.bitria.hexon.utils.TokenManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

interface AuthRepository {
    suspend fun login(request: LoginRequest): LoginResponse
    suspend fun register(request: RegisterRequest): RegisterResponse
    suspend fun refresh(request: RefreshRequest): RefreshResponse
}

class KtorAuthRepository(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) : AuthRepository {
    private val baseUrl = "http://10.0.2.2:8080"

    override suspend fun login(request: LoginRequest): LoginResponse {
        val response = client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<LoginResponse>()
        
        if (response.result == LoginResult.SUCCESS) {
            tokenManager.saveTokens(response.accessToken, response.refreshToken)
        }
        
        return response
    }

    override suspend fun register(request: RegisterRequest): RegisterResponse {
        return client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun refresh(request: RefreshRequest): RefreshResponse {
        val response = client.post("$baseUrl/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<RefreshResponse>()
        
        if (response.result == RefreshResult.SUCCESS) {
            tokenManager.saveTokens(response.accessToken, response.refreshToken)
        }
        
        return response
    }
}
