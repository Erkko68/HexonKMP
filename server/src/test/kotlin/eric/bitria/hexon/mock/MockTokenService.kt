package eric.bitria.hexon.mock

import eric.bitria.hexon.auth.token.TokenService
import java.util.concurrent.atomic.AtomicInteger

class MockTokenService : TokenService {
    companion object {
        private val counter = AtomicInteger(0)
    }

    override fun generateAccessToken(
        userId: String,
    ): String = "access-token-$userId-${counter.getAndIncrement()}"

    override fun generateRefreshToken(
        userId: String
    ): String = "refresh-token-$userId-${counter.getAndIncrement()}"

    override fun verifyToken(token: String): String? {
        if (token.startsWith("refresh-token-")) {
            // Token format: refresh-token-userId-counter
            val content = token.removePrefix("refresh-token-")
            return if (content.contains("-")) {
                content.substringBeforeLast("-")
            } else {
                content
            }
        }
        if (token == "expired-token") {
            return null
        }
        return null
    }
}
