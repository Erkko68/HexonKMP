package eric.bitria.hexon.security

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import kotlin.io.encoding.Base64

@Serializable
data class UserSession(val refreshToken: String)

fun Application.configureSessions() {

    val cookieConfig by inject<CookieConfig>()

    val secretSignKey = Base64.decode(cookieConfig.secret)

    install(Sessions) {
        cookie<UserSession>("USER_SESSION") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = cookieConfig.maxAge.toLong()
            cookie.httpOnly = true
            cookie.secure = true
            cookie.extensions["SameSite"] = "Lax"
            
            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
        }
    }
}
