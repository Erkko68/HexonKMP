package eric.bitria.hexon.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class LogoutRequest(
    val refreshToken: String,
    val logoutAllDevices: Boolean? = false
)

@Serializable
data class LogoutResponse(
    val result: LogoutResult,
    val message: String
)

@Serializable
enum class LogoutResult {
    SUCCESS,
    INVALID_TOKEN,
    UNKNOWN_ERROR
}