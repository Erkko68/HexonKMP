package eric.bitria.hexon.routes

import eric.bitria.hexon.services.auth.login.LoginService
import eric.bitria.hexon.services.auth.refresh.RefreshService
import eric.bitria.hexon.services.auth.register.RegisterService
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.dtos.auth.LogoutRequest
import eric.bitria.hexon.dtos.auth.RefreshRequest
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.security.UserSession
import eric.bitria.hexon.services.auth.logout.LogoutService
import eric.bitria.hexon.utils.toHttpStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import org.koin.ktor.ext.inject

/**
 * Defines the routes for registration, email verification, and resending codes.
 */
fun Route.authRoutes() {
    val registerService by inject<RegisterService>()
    val loginService by inject<LoginService>()
    val refreshService by inject<RefreshService>()
    val logoutService by inject<LogoutService>()

    route("/auth"){
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val response = registerService.register(request)
            call.respond(response.result.toHttpStatus(), response)
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = loginService.login(request)

            // If login succeeded, put the Refresh Token in the Cookie
            if (response.result == LoginResult.SUCCESS && response.refreshToken != null) {
                call.sessions.set(UserSession(refreshToken = response.refreshToken!!))
            }

            call.respond(response.result.toHttpStatus(), response)
        }

        post("/refresh") {
            val requestBody = try {
                call.receiveNullable<RefreshRequest>()
            } catch (e: Exception) {
                null
            }

            // Check body first (Mobile), then Session (Web)
            val refreshToken = if (requestBody?.refreshToken?.isNotBlank() == true) {
                requestBody.refreshToken
            } else {
                call.sessions.get<UserSession>()?.refreshToken
            }

            if (refreshToken.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, "No refresh token found")
                return@post
            }

            val response = refreshService.refresh(RefreshRequest(refreshToken))

            // NEW: Handle Cookie Rotation
            if (response.result == RefreshResult.SUCCESS && response.refreshToken != null) {
                // Update the cookie with the NEW refresh token (will be ignored by mobile if they don't use cookies)
                call.sessions.set(UserSession(refreshToken = response.refreshToken!!))
            } else if (response.result != RefreshResult.SUCCESS) {
                // If refresh failed (token expired/invalid), clear the cookie
                call.sessions.clear<UserSession>()
            }

            call.respond(response.result.toHttpStatus(), response)
        }

        post("/logout") {
            // 1. Try to get it from request body (Mobile)
            val request = call.receive<LogoutRequest>()

            // 2. Fallback to session (Web)
            val refreshToken = request.refreshToken.ifEmpty {
                call.sessions.get<UserSession>()?.refreshToken
            }

            if (refreshToken.isNullOrBlank()) {
                // If no token anywhere, just clear session and return OK (already logged out effectively)
                call.sessions.clear<UserSession>()
                call.respond(HttpStatusCode.OK)
                return@post
            }

            // 3. Call the service to invalidate the session
            val response = logoutService.logout(
                request = LogoutRequest(
                    refreshToken = refreshToken,
                    logoutAllDevices = request.logoutAllDevices ?: false
                )
            )

            // 4. Clear the cookie from the browser
            call.sessions.clear<UserSession>()
            call.respond(response.result.toHttpStatus(), response)
        }
    }
}
