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
    val accessToken: String,
    val refreshToken: String
)

@Serializable
enum class RefreshResult {
    SUCCESS,
    INVALID_TOKEN,
    UNKNOWN_ERROR
}
