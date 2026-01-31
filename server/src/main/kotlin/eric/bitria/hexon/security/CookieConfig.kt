package eric.bitria.hexon.security

import io.ktor.server.config.ApplicationConfig

data class CookieConfig(
    val secret: String,
    val maxAge: String
) {
    companion object {
        fun fromConfig(config: ApplicationConfig): CookieConfig {
            return CookieConfig(
                secret = config.property("cookie.secret").getString(),
                maxAge = config.property("cookie.maxAge").getString()
            )
        }
    }
}
