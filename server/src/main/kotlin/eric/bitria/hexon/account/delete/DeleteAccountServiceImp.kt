package eric.bitria.hexon.account.delete

import eric.bitria.hexon.account.repository.AccountRepository
import eric.bitria.hexon.email.smtp.SmtpService
import eric.bitria.hexon.auth.repository.AuthRepository
import eric.bitria.hexon.dtos.account.DeleteAccountRequest
import eric.bitria.hexon.dtos.account.DeleteAccountResponse
import eric.bitria.hexon.dtos.account.DeleteAccountResult
import eric.bitria.hexon.utils.Validators

class DeleteAccountServiceImp(
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
    private val smtpService: SmtpService
) : DeleteAccountService{
    override suspend fun deleteAccount(request: DeleteAccountRequest): DeleteAccountResponse {
        if(!Validators.isValidPassword(request.password)){
            return DeleteAccountResponse(
                result = DeleteAccountResult.INVALID_PASSWORD,
                message = "Invalid password format."
            )
        }

        if(!Validators.isValidCode(request.code)){
            return DeleteAccountResponse(
                result = DeleteAccountResult.INVALID_CODE,
                message = "Invalid code format."
            )
        }


    }

    override suspend fun deleteAccountCodeRequest(request: DeleteAccountRequest): DeleteAccountResponse {
        TODO("Not yet implemented")
    }
}