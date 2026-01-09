package eric.bitria.hexon.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class ResendVerificationCodeRequest(
    val email: String
)

@Serializable
data class ResendVerificationCodeResponse(
    val result: ResendVerificationCodeResult,
    val message: String
)

@Serializable
enum class ResendVerificationCodeResult {
    SUCCESS,
    ALREADY_VERIFIED,
    UNKNOWN_ERROR
}

