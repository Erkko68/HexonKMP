package eric.bitria.hexon.auth.token

interface TokenService {

    fun generateAccessToken(
        userId: String,
    ): String

    fun generateRefreshToken(
        userId: String
    ): String

    fun verifyToken(token: String): String?
}
