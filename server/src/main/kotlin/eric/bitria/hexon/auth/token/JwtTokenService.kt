package eric.bitria.hexon.auth.token

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.util.Date

class JwtTokenService(
    private val config: JwtConfig
) : TokenService {

    private val algorithm = Algorithm.HMAC256(config.secret)
    private val verifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    override fun generateAccessToken(
        userId: String,
        duration: Long
    ): String {
        val now = System.currentTimeMillis()
        val expiresAt = if (duration > 0) duration else config.accessTokenTtlMillis

        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + expiresAt))
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

    override fun verifyToken(token: String): String? {
        return try {
            val decodedJWT = verifier.verify(token)
            decodedJWT.subject
        } catch (e: JWTVerificationException) {
            null
        }
    }
}
