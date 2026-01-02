package eric.bitria.auth.mock

import eric.bitria.auth.token.TokenService

class MockTokenService : TokenService {

    override fun generateAccessToken(
        userId: String,
    ): String = "access-token-$userId"

    override fun generateRefreshToken(
        userId: String
    ): String = "refresh-token-$userId"
}
