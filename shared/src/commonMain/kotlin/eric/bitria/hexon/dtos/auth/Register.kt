package eric.bitria.hexon.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String
)

@Serializable
data class RegisterResponse(
    val result: RegisterResult,
    val message: String
)

enum class RegisterResult {
    SUCCESS, // Represents "Verification Sent"
    EMAIL_ALREADY_EXISTS,
    USERNAME_ALREADY_EXISTS,
    INVALID_PASSWORD,
    INVALID_EMAIL,
    INVALID_USERNAME,
    UNKNOWN_ERROR
}