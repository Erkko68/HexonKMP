package eric.bitria.auth.token

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

class JwtTokenService(
    private val config: JwtConfig
) : TokenService {

    private val algorithm = Algorithm.HMAC256(config.secret)

    override fun generateAccessToken(
        userId: String
    ): String {
        val now = System.currentTimeMillis()

        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + config.accessTokenTtlMillis))
            .sign(algorithm)
    }

    override fun generateRefreshToken(
        userId: String
    ): String {
        val now = System.currentTimeMillis()

        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + config.refreshTokenTtlMillis))
            .sign(algorithm)
    }
}
