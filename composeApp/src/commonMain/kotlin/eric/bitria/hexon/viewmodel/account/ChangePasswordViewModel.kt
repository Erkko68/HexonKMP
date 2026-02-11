package eric.bitria.hexon.viewmodel.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.data.repository.ApiResult
import eric.bitria.hexon.data.repository.UserRepository
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.utils.Validators
import kotlinx.coroutines.launch

class ChangePasswordViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    var oldPassword by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    var state by mutableStateOf<ApiResult<ChangePasswordResult>>(ApiResult.Idle)
        private set

    var oldPasswordError by mutableStateOf<String?>(null)
        private set

    var passwordError by mutableStateOf<String?>(null)
        private set

    var confirmPasswordError by mutableStateOf<String?>(null)
        private set

    fun onOldPasswordChange(newPassword: String) {
        oldPassword = newPassword
        oldPasswordError = if (Validators.isValidPassword(newPassword)) null else "Invalid password format."
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
        onOldPasswordChange(oldPassword)
        onPasswordChange(password)
        validateConfirmPassword()
        
        return oldPasswordError == null && passwordError == null && confirmPasswordError == null
    }

    fun changePassword() {
        if (!validateForm()) return

        viewModelScope.launch {
            state = ApiResult.Loading
            
            when (val result = userRepository.changePassword(oldPassword, password)) {
                is ApiResult.Success -> {
                    state = when (result.data) {
                        ChangePasswordResult.SUCCESS -> {
                            ApiResult.Success(ChangePasswordResult.SUCCESS)
                        }
                        ChangePasswordResult.WRONG_PASSWORD -> ApiResult.Error("Old password is incorrect.")
                        ChangePasswordResult.INVALID_PASSWORD -> ApiResult.Error("New password format is invalid.")
                        ChangePasswordResult.USER_NOT_FOUND -> ApiResult.Error("User session not found.")
                        else -> ApiResult.Error("An unexpected error occurred.")
                    }
                }
                is ApiResult.NetworkError -> {
                    state = ApiResult.NetworkError
                }
                is ApiResult.Error -> {
                    state = ApiResult.Error(result.message ?: "Failed to change password.")
                }
                else -> {}
            }
        }
    }

    fun resetState() {
        state = ApiResult.Idle
    }
}
