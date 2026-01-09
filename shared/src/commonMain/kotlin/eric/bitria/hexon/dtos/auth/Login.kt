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
    val accessToken: String? = null,
    val refreshToken: String? = null
)

@Serializable
enum class LoginResult {
    SUCCESS,
    INVALID_CREDENTIALS,
    NOT_VERIFIED,
    UNKNOWN_ERROR
}