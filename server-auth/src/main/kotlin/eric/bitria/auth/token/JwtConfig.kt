package eric.bitria.auth.token

data class JwtConfig(
    val issuer: String,
    val audience: String,
    val secret: String,
    val accessTokenTtlMillis: Long,
    val refreshTokenTtlMillis: Long
)

