package eric.bitria.hexon.repository

import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse
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

class KtorAuthRepository(private val client: HttpClient) : AuthRepository {
    private val baseUrl = "http://10.0.2.2:8080" // Use 10.0.2.2 for Android emulator to hit localhost

    override suspend fun login(request: LoginRequest): LoginResponse {
        return client.post("$baseUrl/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun register(request: RegisterRequest): RegisterResponse {
        return client.post("$baseUrl/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun refresh(request: RefreshRequest): RefreshResponse {
        return client.post("$baseUrl/refresh") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
