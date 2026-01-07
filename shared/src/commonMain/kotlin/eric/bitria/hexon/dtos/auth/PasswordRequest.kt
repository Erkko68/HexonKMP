package eric.bitria.hexon.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequest(
    val email: String,

    // Provided if the user forgot their password (received via email)
    val resetCode: String? = null,

    // Provided if the user is logged in and knows their current password
    val oldPassword: String? = null,

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
    INVALID_PASSWORD_OR_CODE,
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
    UNKNOWN_ERROR
}