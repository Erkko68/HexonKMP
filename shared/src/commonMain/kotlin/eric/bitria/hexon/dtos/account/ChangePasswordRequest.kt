package eric.bitria.hexon.dtos.account

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
    INVALID_PASSWORD_FORMAT,
    INVALID_PASSWORD,
    UNKNOWN_ERROR
}

@Serializable
data class ResetPasswordRequest(
    val code: String,
    val email: String,
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
    UNKNOWN_EMAIL,
    INVALID_EMAIL_FORMAT,
    INVALID_CODE_FORMAT,
    INVALID_PASSWORD_FORMAT,
    INVALID_CODE,
    UNKNOWN_ERROR
}