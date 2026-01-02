package eric.bitria.auth.token

interface TokenService {

    fun generateAccessToken(
        userId: String,
    ): String

    fun generateRefreshToken(
        userId: String
    ): String
}
