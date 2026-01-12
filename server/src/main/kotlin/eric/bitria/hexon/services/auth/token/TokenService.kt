package eric.bitria.hexon.services.auth.token


interface TokenService {

    /**
     * Generates a short-lived JWT Access Token.
     * Contains the user's ID as claims.
     * * @param userId The unique identifier of the user (Subject).
     * @return A signed JWT string valid for a short duration (e.g., 15 mins).
     */
    fun generateAccessToken(
        userId: String
    ): String

    /**
     * Generates a long-lived JWT Refresh Token.
     * Contains minimal claims (usually just the User ID).
     * Used solely to obtain new Access Tokens.
     * * @param userId The unique identifier of the user.
     * @return A signed JWT string valid for a long duration.
     */
    fun generateRefreshToken(
        userId: String
    ): String

    /**
     * Validates the signature and expiration of a Refresh Token.
     * Does NOT check the database for revocation (that is the Service's job).
     * * @param token The raw JWT string to verify.
     * @return The userId extracted from the token if valid, or null if invalid/expired.
     */
    fun validateRefreshToken(token: String): String?
}