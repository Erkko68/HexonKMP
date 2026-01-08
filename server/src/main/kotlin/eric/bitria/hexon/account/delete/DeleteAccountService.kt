package eric.bitria.hexon.account.delete

import eric.bitria.hexon.dtos.account.DeleteAccountRequest
import eric.bitria.hexon.dtos.account.DeleteAccountResponse

interface DeleteAccountService {
    suspend fun deleteAccount(request: DeleteAccountRequest): DeleteAccountResponse
    suspend fun deleteAccountCodeRequest(request: DeleteAccountRequest): DeleteAccountResponse
}