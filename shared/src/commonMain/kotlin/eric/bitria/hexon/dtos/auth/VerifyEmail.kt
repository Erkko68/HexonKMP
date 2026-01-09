package eric.bitria.hexon.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class VerifyEmailRequest(
    val email: String,
    val code: String
)

@Serializable
data class VerifyEmailResponse(
    val result: VerifyEmailResult,
    val message: String
)

@Serializable
enum class VerifyEmailResult {
    SUCCESS,
    INVALID_CODE,   // Code is wrong or expired
    USER_NOT_FOUND, // Email doesn't exist in our DB
    ALREADY_VERIFIED, // Account is already active
    UNKNOWN_ERROR
}