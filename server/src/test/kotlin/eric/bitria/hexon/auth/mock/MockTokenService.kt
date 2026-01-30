package eric.bitria.hexon.auth.mock

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eric.bitria.hexon.services.auth.token.TokenService
import java.util.*

class MockTokenService : TokenService {
    private val secret = "test-secret"
    private val issuer = "hexon-test"
    private val audience = "hexon-test-audience"
    private val algorithm = Algorithm.HMAC256(secret)

    override fun generateAccessToken(userId: String, username: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
            .sign(algorithm)
    }

    override fun generateRefreshToken(userId: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId)
            .withJWTId(UUID.randomUUID().toString())
            .withExpiresAt(Date(System.currentTimeMillis() + 86400000))
            .sign(algorithm)
    }

    override fun validateRefreshToken(token: String): String? {
        return try {
            val verifier = JWT.require(algorithm).withIssuer(issuer).build()
            val decoded = verifier.verify(token)
            decoded.subject
        } catch (e: Exception) {
            null
        }
    }
}
