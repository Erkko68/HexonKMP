package eric.bitria.hexon.services.auth.token

import io.ktor.server.config.ApplicationConfig

data class JwtConfig(
    val issuer: String,
    val audience: String,
    val secret: String,
    val realm: String,
    val accessTokenTtlMillis: Long,
    val refreshTokenTtlMillis: Long
) {
    companion object {
        fun fromConfig(config: ApplicationConfig): JwtConfig {
            return JwtConfig(
                issuer = config.property("jwt.issuer").getString(),
                audience = config.property("jwt.audience").getString(),
                secret = config.property("jwt.secret").getString(),
                realm = config.property("jwt.realm").getString(),
                accessTokenTtlMillis = config.property("jwt.accessTokenTtlMillis").getString().toLong(),
                refreshTokenTtlMillis = config.property("jwt.refreshTokenTtlMillis").getString().toLong()
            )
        }
    }
}
