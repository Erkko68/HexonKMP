package eric.bitria.hexon.viewmodel.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.client.UserClient
import eric.bitria.hexon.client.persistence.AccountManager
import eric.bitria.hexon.client.persistence.SettingsManager
import eric.bitria.hexon.client.persistence.token.TokenManager
import eric.bitria.hexon.dtos.account.ConfirmDeleteAccountRequest
import eric.bitria.hexon.dtos.account.DeleteAccountResult
import eric.bitria.hexon.utils.Validators
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class DeleteAccountViewModel(
    private val userClient: UserClient,
    private val tokenManager: TokenManager,
    private val accountManager: AccountManager,
    private val settingsManager: SettingsManager
) : ViewModel() {

    var password by mutableStateOf("")
        private set

    var code by mutableStateOf("")
        private set

    var state by mutableStateOf(DeleteAccountStatus.IDLE)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var passwordError by mutableStateOf<String?>(null)
        private set

    var codeError by mutableStateOf<String?>(null)
        private set

    var codeSent by mutableStateOf(false)
        private set

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        passwordError = if (Validators.isValidPassword(newPassword)) null else "Password must be 8-32 characters, with at least one uppercase, one lowercase, and one digit."
    }

    fun onCodeChange(newCode: String) {
        if (newCode.length <= 6) {
            code = newCode
            codeError = if (Validators.isValidCode(newCode)) null else "Code must be 6 digits."
        }
    }

    fun initiateDelete() {
        viewModelScope.launch {
            state = DeleteAccountStatus.LOADING
            errorMessage = null
            try {
                withTimeout(10000L) {
                    userClient.initiateDeleteAccount()
                    codeSent = true
                    state = DeleteAccountStatus.IDLE
                }
            } catch (e: Exception) {
                state = DeleteAccountStatus.ERROR
                errorMessage = "Failed to send verification code: ${e.message}"
            }
        }
    }

    fun confirmDelete() {
        val isPasswordValid = Validators.isValidPassword(password)
        val isCodeValid = Validators.isValidCode(code)

        if (!isPasswordValid) passwordError = "Invalid password format."
        if (!isCodeValid) codeError = "Code must be 6 digits."

        if (!isPasswordValid || !isCodeValid) return

        viewModelScope.launch {
            state = DeleteAccountStatus.LOADING
            errorMessage = null
            try {
                withTimeout(10000L) {
                    val response = userClient.confirmDeleteAccount(
                        ConfirmDeleteAccountRequest(
                            password = password,
                            code = code
                        )
                    )
                    when (response.result) {
                        DeleteAccountResult.SUCCESS -> {
                            clearUserData()
                            state = DeleteAccountStatus.SUCCESS
                        }
                        else -> {
                            state = DeleteAccountStatus.ERROR
                            errorMessage = response.message
                        }
                    }
                }
            } catch (e: Exception) {
                state = DeleteAccountStatus.ERROR
                errorMessage = "Failed to delete account: ${e.message}"
            }
        }
    }

    fun resendCode() {
        initiateDelete()
    }

    private fun clearUserData() {
        tokenManager.clearTokens()
        accountManager.clear()
        settingsManager.clear()
    }

    fun resetState() {
        state = DeleteAccountStatus.IDLE
        errorMessage = null
        passwordError = null
        codeError = null
    }
}

enum class DeleteAccountStatus {
    IDLE, LOADING, SUCCESS, ERROR
}
