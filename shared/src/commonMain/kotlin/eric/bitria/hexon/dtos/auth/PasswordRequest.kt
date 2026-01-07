package eric.bitria.hexon.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

@Serializable
data class ChangePasswordResponse(
    val result: ChangePasswordResult,
    val message: String
)

@Serializable
enum class ChangePasswordResult {
    SUCCESS,
    INVALID_PASSWORD,
    UNKNOWN_ERROR
}

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