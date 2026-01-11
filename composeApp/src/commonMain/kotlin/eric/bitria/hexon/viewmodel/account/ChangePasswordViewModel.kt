package eric.bitria.hexon.viewmodel.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.client.UserClient
import eric.bitria.hexon.client.SessionManager
import eric.bitria.hexon.utils.Validators
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ChangePasswordViewModel(
    private val userClient: UserClient,
    private val sessionManager: SessionManager
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var oldPassword by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    var state by mutableStateOf(ChangePasswordStatus.IDLE)
        private set

    var errorMessage by mutableStateOf<String?>(null)
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
            state = ChangePasswordStatus.LOADING
            errorMessage = null
            try {
                withTimeout(10000L) {
                    val response = userClient.changePassword(
                        ChangePasswordRequest(
                            oldPassword = oldPassword,
                            newPassword = password
                        )
                    )
                    when (response.result) {
                        ChangePasswordResult.SUCCESS -> {
                            state = ChangePasswordStatus.SUCCESS
                            sessionManager.logout()
                        }
                        else -> {
                            state = ChangePasswordStatus.ERROR
                            println(response.message)
                            errorMessage = response.message
                        }
                    }
                }
            } catch (e: Exception) {
                state = ChangePasswordStatus.ERROR
                errorMessage = "Failed to update password: ${e.message}"
            }
        }
    }
}

enum class ChangePasswordStatus {
    IDLE, LOADING, SUCCESS, ERROR
}
