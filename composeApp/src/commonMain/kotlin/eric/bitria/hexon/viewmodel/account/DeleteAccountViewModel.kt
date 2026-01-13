package eric.bitria.hexon.viewmodel.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.api.client.SessionManager
import eric.bitria.hexon.dtos.account.DeleteAccountResult
import eric.bitria.hexon.ui.repository.ApiResult
import eric.bitria.hexon.ui.repository.UserRepository
import eric.bitria.hexon.utils.Validators
import kotlinx.coroutines.launch

class DeleteAccountViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    var password by mutableStateOf("")
        private set

    var code by mutableStateOf("")
        private set

    var state by mutableStateOf<ApiResult<DeleteAccountResult>>(ApiResult.Idle)
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
            state = ApiResult.Loading
            
            when (val result = userRepository.requestDeleteAccount()) {
                is ApiResult.Success -> {
                    codeSent = true
                    state = ApiResult.Idle // Move to Idle to allow confirmDelete interaction
                }
                is ApiResult.NetworkError -> {
                    state = ApiResult.NetworkError
                }
                is ApiResult.Error -> {
                    state = ApiResult.Error(result.message ?: "Failed to send verification code.")
                }
                else -> {}
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
            state = ApiResult.Loading
            
            when (val result = userRepository.deleteAccount(password, code)) {
                is ApiResult.Success -> {
                    state = when (result.data) {
                        DeleteAccountResult.SUCCESS -> {
                            sessionManager.logout()
                            ApiResult.Success(DeleteAccountResult.SUCCESS)
                        }
                        DeleteAccountResult.WRONG_PASSWORD -> ApiResult.Error("Incorrect password.")
                        DeleteAccountResult.INVALID_CODE -> ApiResult.Error("Invalid verification code.")
                        DeleteAccountResult.USER_NOT_FOUND -> ApiResult.Error("User session not found.")
                        else -> ApiResult.Error("An unexpected error occurred.")
                    }
                }
                is ApiResult.NetworkError -> {
                    state = ApiResult.NetworkError
                }
                is ApiResult.Error -> {
                    state = ApiResult.Error(result.message ?: "Failed to delete account.")
                }
                else -> {}
            }
        }
    }

    fun resendCode() {
        initiateDelete()
    }

    fun resetState() {
        state = ApiResult.Idle
    }
}
