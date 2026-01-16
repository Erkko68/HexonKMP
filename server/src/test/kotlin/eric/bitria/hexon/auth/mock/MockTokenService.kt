package eric.bitria.hexon.auth.mock

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eric.bitria.hexon.services.auth.token.TokenService
import java.util.Date
import kotlin.random.Random

class MockTokenService : TokenService {
    private val secret = "secret"
    private val algorithm = Algorithm.HMAC256(secret)

    override fun generateAccessToken(userId: String, username: String): String {
        return JWT.create()
            .withSubject(userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
            .sign(algorithm)
    }

    override fun generateRefreshToken(userId: String): String {
        // We use a prefix to help validateRefreshToken identify it in this mock
        return "mock-refresh-token-" + JWT.create()
            .withSubject(userId)
            .withClaim("nonce", Random.nextInt())
            .withExpiresAt(Date(System.currentTimeMillis() + 86400000))
            .sign(algorithm)
    }

    override fun validateRefreshToken(token: String): String? {
        return try {
            val jwtString = if (token.startsWith("mock-refresh-token-")) {
                token.removePrefix("mock-refresh-token-")
            } else {
                token
            }
            val verifier = JWT.require(algorithm).build()
            val decoded = verifier.verify(jwtString)
            decoded.subject
        } catch (e: Exception) {
            null
        }
    }
}
