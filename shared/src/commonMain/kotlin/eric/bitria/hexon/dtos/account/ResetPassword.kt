package eric.bitria.hexon.dtos.account

import kotlinx.serialization.Serializable

// --- STEP 1: REQUEST CODE ---
@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class ForgotPasswordResponse(
    val result: ForgotPasswordResult,
    val message: String
)

@Serializable
enum class ForgotPasswordResult {
    SUCCESS,
    INVALID_EMAIL,
    UNKNOWN_ERROR
}

// --- STEP 2: CONFIRM RESET ---
@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

@Serializable
data class ResetPasswordResponse(
    val result: ResetPasswordResult,
    val message: String
)

@Serializable
enum class ResetPasswordResult {
    SUCCESS,
    INVALID_CODE,
    INVALID_EMAIL,
    USER_NOT_FOUND,
    INVALID_PASSWORD,
    UNKNOWN_ERROR
}