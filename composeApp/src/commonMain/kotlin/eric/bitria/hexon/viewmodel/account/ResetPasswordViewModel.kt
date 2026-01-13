package eric.bitria.hexon.viewmodel.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.api.SessionManager
import eric.bitria.hexon.dtos.account.ResetPasswordResult
import eric.bitria.hexon.api.repository.ApiResult
import eric.bitria.hexon.api.repository.UserRepository
import eric.bitria.hexon.utils.Validators
import kotlinx.coroutines.launch

class ResetPasswordViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var resetCode by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    var state by mutableStateOf<ApiResult<ResetPasswordResult>>(ApiResult.Idle)
        private set

    var resetCodeError by mutableStateOf<String?>(null)
        private set

    var passwordError by mutableStateOf<String?>(null)
        private set

    var confirmPasswordError by mutableStateOf<String?>(null)
        private set

    fun init(email: String) {
        this.email = email
    }

    fun onResetCodeChange(newCode: String) {
        resetCode = newCode
        resetCodeError = if (Validators.isValidCode(newCode)) null else "Code must be 6 digits."
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        passwordError = if (Validators.isValidPassword(newPassword)) null 
                       else "Password must be 8-32 characters, with at least one uppercase, one lowercase, and one digit."
        if (confirmPassword.isNotEmpty()) {
            validateConfirmPassword()
        }
    }

    fun onConfirmPasswordChange(newConfirmPassword: String) {
        confirmPassword = newConfirmPassword
        validateConfirmPassword()
    }

    private fun validateConfirmPassword() {
        confirmPasswordError = if (confirmPassword == password) null else "Passwords do not match."
    }

    private fun validateForm(): Boolean {
        onResetCodeChange(resetCode)
        onPasswordChange(password)
        validateConfirmPassword()
        
        return resetCodeError == null && passwordError == null && confirmPasswordError == null
    }

    fun resetPassword() {
        if (!validateForm()) return

        viewModelScope.launch {
            state = ApiResult.Loading
            
            when (val result = userRepository.resetPassword(email, resetCode, password)) {
                is ApiResult.Success -> {
                    state = when (result.data) {
                        ResetPasswordResult.SUCCESS -> {
                            sessionManager.logout()
                            ApiResult.Success(ResetPasswordResult.SUCCESS)
                        }
                        ResetPasswordResult.INVALID_CODE -> ApiResult.Error("Invalid reset code.")
                        ResetPasswordResult.INVALID_EMAIL -> ApiResult.Error("The provided email is invalid.")
                        ResetPasswordResult.INVALID_PASSWORD -> ApiResult.Error("The new password format is invalid.")
                        ResetPasswordResult.USER_NOT_FOUND -> ApiResult.Error("User not found.")
                        else -> ApiResult.Error("An unexpected error occurred.")
                    }
                }
                is ApiResult.NetworkError -> {
                    state = ApiResult.NetworkError
                }
                is ApiResult.Error -> {
                    state = ApiResult.Error(result.message ?: "Failed to reset password.")
                }
                else -> {}
            }
        }
    }

    fun resetState() {
        state = ApiResult.Idle
    }
}
