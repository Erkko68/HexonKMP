package eric.bitria.hexon.auth.mock

import eric.bitria.hexon.auth.token.TokenService
import java.util.concurrent.atomic.AtomicInteger

class MockTokenService : TokenService {
    companion object {
        private val counter = AtomicInteger(0)
    }

    override fun generateAccessToken(
        userId: String
    ): String = "access-token-$userId-${counter.getAndIncrement()}"

    override fun generateRefreshToken(
        userId: String
    ): String = "refresh-token-$userId-${counter.getAndIncrement()}"

    override fun validateRefreshToken(token: String): String? {
        if (token.startsWith("refresh-token-") || token.startsWith("access-token-") || token.startsWith("reset-token-")) {
            val prefix = when {
                token.startsWith("refresh-token-") -> "refresh-token-"
                token.startsWith("access-token-") -> "access-token-"
                token.startsWith("reset-token-") -> "reset-token-"
                else -> ""
            }
            val content = token.removePrefix(prefix)
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
