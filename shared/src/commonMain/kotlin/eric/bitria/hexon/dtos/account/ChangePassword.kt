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
    WRONG_PASSWORD,     // Old password didn't match
    INVALID_PASSWORD,   // New password is too weak (optional)
    USER_NOT_FOUND,
    UNKNOWN_ERROR
}