package eric.bitria.hexon.security

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.util.decodeBase64Bytes
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class UserSession(val refreshToken: String)

fun Application.configureSessions() {

    val cookieConfig by inject<CookieConfig>()

    val secretSignKey = cookieConfig.secret.decodeBase64Bytes()

    install(Sessions) {
        cookie<UserSession>("USER_SESSION") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 60 * 60 * 24 * 7 // 7 days
            cookie.httpOnly = true
            cookie.secure = false // Set to true in production with HTTPS
            
            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
        }
    }
}
