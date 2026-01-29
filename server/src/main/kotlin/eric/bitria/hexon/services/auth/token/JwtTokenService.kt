package eric.bitria.hexon.services.auth.token

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import eric.bitria.hexon.security.JwtConfig
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
        username: String
    ): String {
        val now = System.currentTimeMillis()

        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId)
            .withClaim("username", username)
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

    override fun validateRefreshToken(token: String): String? {
        return try {
            val decodedJWT = verifier.verify(token)
            decodedJWT.subject
        } catch (e: JWTVerificationException) {
            null
        }
    }


}
