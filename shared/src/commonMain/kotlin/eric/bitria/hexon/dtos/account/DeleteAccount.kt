package eric.bitria.hexon.dtos.account

import kotlinx.serialization.Serializable

// --- STEP 1: REQUEST ---
// No request body needed, the JWT implies "Me"
@Serializable
data class RequestDeleteAccountResponse(
    val message: String
)

// --- STEP 2: CONFIRM ---
@Serializable
data class ConfirmDeleteAccountRequest(
    val password: String,
    val code: String
)

@Serializable
data class ConfirmDeleteAccountResponse(
    val result: DeleteAccountResult,
    val message: String
)

enum class DeleteAccountResult {
    SUCCESS,
    WRONG_PASSWORD,
    INVALID_CODE,
    USER_NOT_FOUND,
    UNKNOWN_ERROR
}