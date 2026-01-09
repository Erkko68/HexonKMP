package eric.bitria.hexon.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class RefreshResponse(
    val result: RefreshResult,
    val message: String,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

enum class RefreshResult {
    SUCCESS,
    INVALID_TOKEN,      // Token is malformed, expired, or JWT signature is wrong
    TOKEN_MISMATCH,     // Token is a valid JWT but does not match the DB hash (Session hijacking/Reuse)
    USER_NOT_FOUND,     // User deleted while session was active
    UNKNOWN_ERROR
}