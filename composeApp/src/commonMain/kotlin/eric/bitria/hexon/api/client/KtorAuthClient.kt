package eric.bitria.hexon.api.client

import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse
import eric.bitria.hexon.dtos.auth.LogoutRequest
import eric.bitria.hexon.dtos.auth.LogoutResponse
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

interface AuthClient {
    suspend fun register(request: RegisterRequest): RegisterResponse
    suspend fun login(request: LoginRequest): LoginResponse
    suspend fun refresh(): RefreshResponse
    suspend fun logout(request: LogoutRequest): LogoutResponse
}

class KtorAuthClient(
    private val client: HttpClient
) : AuthClient {

    override suspend fun register(request: RegisterRequest): RegisterResponse {
        return client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun login(request: LoginRequest): LoginResponse {
        return client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun refresh(): RefreshResponse {
        return client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
        }.body()
    }

    override suspend fun logout(request: LogoutRequest): LogoutResponse {
        return client.post("/auth/logout") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

}
