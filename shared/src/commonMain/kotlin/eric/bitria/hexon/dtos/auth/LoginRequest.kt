package eric.bitria.hexon.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val result: LoginResult,
    val message: String,
    val accessToken: String,
    val refreshToken: String
)

@Serializable
enum class LoginResult {
    SUCCESS,
    INVALID_EMAIL_OR_PASSWORD,
    PENDING_VERIFICATION,
    UNKNOWN_ERROR
}