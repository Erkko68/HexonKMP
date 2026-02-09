package eric.bitria.hexon.services.auth.logout

import eric.bitria.hexon.dtos.auth.LogoutRequest
import eric.bitria.hexon.dtos.auth.LogoutResponse

interface LogoutService {
    /**
     * Handles the logout logic.
     * * @param refreshToken The token from the HTTP Cookie (used to identify the current session).
     * @param request The request body containing flags (e.g., logoutAllDevices).
     */
    suspend fun logout(request: LogoutRequest): LogoutResponse
}