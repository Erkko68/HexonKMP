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
            val session = call.sessions.get<UserSession>()
            val refreshToken = session?.refreshToken

            if (refreshToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "No session cookie found")
                return@post
            }

            // Manually create the request object using the token from the cookie
            val request = RefreshRequest(refreshToken)
            val response = refreshService.refresh(request)

            // NEW: Handle Cookie Rotation
            if (response.result == RefreshResult.SUCCESS && response.refreshToken != null) {
                // Update the cookie with the NEW refresh token
                call.sessions.set(UserSession(refreshToken = response.refreshToken!!))
            } else {
                // If refresh failed (token expired/invalid), clear the cookie
                call.sessions.clear<UserSession>()
            }

            call.respond(response.result.toHttpStatus(), response)
        }

        post("/logout") {
            // 1. Get the session (Cookie)
            val session = call.sessions.get<UserSession>()
            val refreshToken = session?.refreshToken

            if (refreshToken == null) {
                // No cookie = Already logged out
                call.respond(HttpStatusCode.OK)
                return@post
            }

            // 2. Get the request body
            // We use receiveNullable because a simple "Logout" button
            // might send an empty body, which is valid (defaults to logoutAllDevices=false).
            val request = try {
                call.receiveNullable<LogoutRequest>() ?: LogoutRequest()
            } catch (e: Exception) {
                LogoutRequest()
            }

            // 3. Call the service
            val response = logoutService.logout(refreshToken, request)

            // 4. Clear the cookie from the browser
            call.sessions.clear<UserSession>()
            call.respond(response.result.toHttpStatus(), response)
        }
    }
}
