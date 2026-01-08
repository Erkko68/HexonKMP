package eric.bitria.hexon.viewmodel.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.dtos.account.ForgotPasswordRequest
import eric.bitria.hexon.dtos.account.ForgotPasswordResult
import eric.bitria.hexon.client.repository.AccountRepository
import eric.bitria.hexon.utils.Validators
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class SendPasswordResetCodeViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var state by mutableStateOf(ForgotPasswordStatus.IDLE)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var emailError by mutableStateOf<String?>(null)
        private set

    fun onEmailChange(newEmail: String) {
        email = newEmail
        emailError = if (Validators.isValidEmail(newEmail)) null else "Invalid email format."
    }

    private fun validateForm(): Boolean {
        onEmailChange(email)
        return emailError == null
    }

    fun forgotPassword() {
        if (!validateForm()) return

        viewModelScope.launch {
            state = ForgotPasswordStatus.LOADING
            errorMessage = null
            try {
                withTimeout(10000L) {
                    val response = accountRepository.forgotPassword(ForgotPasswordRequest(email))
                    when (response.result) {
                        ForgotPasswordResult.SUCCESS -> {
                            state = ForgotPasswordStatus.SUCCESS
                        }
                        else -> {
                            state = ForgotPasswordStatus.ERROR
                            errorMessage = response.message
                        }
                    }
                }
            } catch (e: Exception) {
                state = ForgotPasswordStatus.ERROR
                errorMessage = "Failed to send reset code: ${e.message}"
            }
        }
    }

    fun resetState() {
        state = ForgotPasswordStatus.IDLE
    }
}

enum class ForgotPasswordStatus {
    IDLE, LOADING, SUCCESS, ERROR
}
