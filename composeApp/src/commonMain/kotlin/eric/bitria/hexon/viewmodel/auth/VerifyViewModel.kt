package eric.bitria.hexon.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.api.client.SessionManager
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import eric.bitria.hexon.ui.repository.ApiResult
import eric.bitria.hexon.ui.repository.UserRepository
import kotlinx.coroutines.launch

class VerifyViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var code by mutableStateOf("")
        private set

    var verifyStatus by mutableStateOf<ApiResult<VerifyEmailResult>>(ApiResult.Idle)
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
            verifyStatus = ApiResult.Error("Code must be 6 digits")
            return
        }

        viewModelScope.launch {
            verifyStatus = ApiResult.Loading

            when (val result = userRepository.verifyEmail(code, email)) {
                is ApiResult.Success -> {
                    verifyStatus = when (result.data) {
                        VerifyEmailResult.SUCCESS -> {
                            sessionManager.login()
                            ApiResult.Success(VerifyEmailResult.SUCCESS)
                        }
                        VerifyEmailResult.INVALID_CODE -> ApiResult.Error("Invalid verification code.")
                        VerifyEmailResult.ALREADY_VERIFIED -> ApiResult.Error("Email is already verified.")
                        VerifyEmailResult.USER_NOT_FOUND -> ApiResult.Error("User not found.")
                        else -> ApiResult.Error("An unexpected error occurred.")
                    }
                }
                is ApiResult.NetworkError -> {
                    verifyStatus = ApiResult.NetworkError
                }
                is ApiResult.Error -> {
                    verifyStatus = ApiResult.Error(result.message ?: "Verification failed.")
                }
                else -> {}
            }
        }
    }

    fun resendCode() {
        viewModelScope.launch {
            userRepository.resendVerificationCode(email)
        }
    }

    fun resetState() {
        verifyStatus = ApiResult.Idle
    }
}
