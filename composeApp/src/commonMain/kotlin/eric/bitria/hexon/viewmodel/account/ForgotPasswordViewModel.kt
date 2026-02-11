package eric.bitria.hexon.viewmodel.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.data.repository.ApiResult
import eric.bitria.hexon.data.repository.UserRepository
import eric.bitria.hexon.dtos.account.ForgotPasswordResult
import eric.bitria.hexon.utils.Validators
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var state by mutableStateOf<ApiResult<ForgotPasswordResult>>(ApiResult.Idle)
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
            state = ApiResult.Loading
            
            when (val result = userRepository.forgotPassword(email)) {
                is ApiResult.Success -> {
                    state = when (result.data) {
                        ForgotPasswordResult.SUCCESS -> ApiResult.Success(ForgotPasswordResult.SUCCESS)
                        ForgotPasswordResult.INVALID_EMAIL -> ApiResult.Error("The provided email is invalid.")
                        else -> ApiResult.Error("An unexpected error occurred.")
                    }
                }
                is ApiResult.NetworkError -> {
                    state = ApiResult.NetworkError
                }
                is ApiResult.Error -> {
                    state = ApiResult.Error(result.message ?: "Failed to send reset code.")
                }
                else -> {}
            }
        }
    }

    fun resetState() {
        state = ApiResult.Idle
    }
}
