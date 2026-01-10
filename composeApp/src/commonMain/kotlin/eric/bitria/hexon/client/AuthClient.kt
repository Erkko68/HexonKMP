package eric.bitria.hexon.client

import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResponse
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResponse

interface AuthClient {
    suspend fun register(request: RegisterRequest): RegisterResponse
    suspend fun login(request: LoginRequest): LoginResponse
    suspend fun refresh(request: RefreshRequest): RefreshResponse
}