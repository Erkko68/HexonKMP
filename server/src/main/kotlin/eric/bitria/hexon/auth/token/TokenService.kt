package eric.bitria.hexon.auth.token

interface TokenService {

    fun generateAccessToken(
        userId: String,
        duration: Long = -1
    ): String

    fun generateRefreshToken(
        userId: String
    ): String

    fun verifyToken(token: String): String?
}
