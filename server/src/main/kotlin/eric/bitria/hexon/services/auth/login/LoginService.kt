package eric.bitria.hexon.services.auth.login

import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResponse

interface LoginService {
    /**
     * Authenticates a user.
     * 1. Verifies credentials (BCrypt).
     * 2. Checks if account is verified.
     * 3. Generates Access & Refresh tokens.
     * 4. Stores the Refresh Token hash in the DB.
     */
    suspend fun login(request: LoginRequest): LoginResponse
}