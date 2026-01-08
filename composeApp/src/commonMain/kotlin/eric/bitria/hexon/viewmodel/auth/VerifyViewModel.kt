package eric.bitria.hexon.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.dtos.auth.*
import eric.bitria.hexon.client.repository.AuthRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class VerifyViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var code by mutableStateOf("")
        private set

    var verifyStatus by mutableStateOf(VerifyStatus.IDLE)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun updateEmail(email: String) {
        this.email = email
    }

    fun onCodeChange(newCode: String) {
        if (newCode.length <= 6 && newCode.all { it.isDigit() }) {
            code = newCode
        }
    }

    fun verify() {
        if (code.length != 6) {
            errorMessage = "Code must be 6 digits"
            return
        }

        viewModelScope.launch {
            verifyStatus = VerifyStatus.LOADING
            errorMessage = null
            try {
                withTimeout(10000L) {
                    val response = authRepository.verifyEmail(VerifyEmailRequest(email, code))
                    when (response.result) {
                        VerifyEmailResult.SUCCESS -> {
                            verifyStatus = VerifyStatus.SUCCESS
                        }
                        else -> {
                            verifyStatus = VerifyStatus.ERROR
                            errorMessage = response.message
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                verifyStatus = VerifyStatus.TIMEOUT
                errorMessage = "Request timed out. Please try again."
            } catch (e: Exception) {
                verifyStatus = VerifyStatus.ERROR
                errorMessage = "Verification failed: ${e.message}"
            }
        }
    }

    fun resendCode() {
        viewModelScope.launch {
            try {
                authRepository.resendVerificationCode(ResendVerificationCodeRequest(email))
                // Optionally show a message that code was resent
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

enum class VerifyStatus {
    IDLE, LOADING, SUCCESS, ERROR, TIMEOUT
}