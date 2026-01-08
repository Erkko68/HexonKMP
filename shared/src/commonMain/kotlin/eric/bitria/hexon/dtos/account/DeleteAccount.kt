package eric.bitria.hexon.dtos.account

import kotlinx.serialization.Serializable

@Serializable
data class DeleteAccountRequest(
    val password: String,
    val code: String
)

@Serializable
data class DeleteAccountResponse(
    val result: DeleteAccountResult,
    val message: String
)

@Serializable
enum class DeleteAccountResult {
    SUCCESS,
    INVALID_CODE,
    INVALID_PASSWORD,
    UNKNOWN_ERROR
}