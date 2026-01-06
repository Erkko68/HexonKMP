package eric.bitria.hexon.auth.login

import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse

interface LoginService {
    suspend fun login(request: LoginRequest): LoginResponse
}