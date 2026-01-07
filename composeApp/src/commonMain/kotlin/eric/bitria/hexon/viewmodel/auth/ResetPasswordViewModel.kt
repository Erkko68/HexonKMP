package eric.bitria.hexon.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.dtos.auth.ChangePasswordRequest
import eric.bitria.hexon.dtos.auth.ChangePasswordResult
import eric.bitria.hexon.repository.AuthRepository
import eric.bitria.hexon.utils.Validators
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ResetPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var resetCode by mutableStateOf("")
        private set

    var oldPassword by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    var state by mutableStateOf(ResetPasswordStatus.IDLE)
        private set

    var isResetMode by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var resetCodeError by mutableStateOf<String?>(null)
        private set

    var oldPasswordError by mutableStateOf<String?>(null)
        private set

    var passwordError by mutableStateOf<String?>(null)
        private set

    var confirmPasswordError by mutableStateOf<String?>(null)
        private set

    fun init(email: String, isResetMode: Boolean) {
        this.email = email
        this.isResetMode = isResetMode
    }

    fun onResetCodeChange(newCode: String) {
        resetCode = newCode
        resetCodeError = if (Validators.isValidCode(newCode)) null else "Code must be 6 digits."
    }

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
        if (isResetMode) {
            onResetCodeChange(resetCode)
        } else {
            onOldPasswordChange(oldPassword)
        }
        onPasswordChange(password)
        validateConfirmPassword()
        
        return (if (isResetMode) resetCodeError == null else oldPasswordError == null) && 
                passwordError == null && confirmPasswordError == null
    }

    fun changePassword() {
        if (!validateForm()) return

        viewModelScope.launch {
            state = ResetPasswordStatus.LOADING
            errorMessage = null
            try {
                withTimeout(10000L) {
                    val response = authRepository.changePassword(
                        ChangePasswordRequest(
                            email = email,
                            resetCode = if (isResetMode) resetCode else null,
                            oldPassword = if (!isResetMode) oldPassword else null,
                            newPassword = password
                        )
                    )
                    when (response.result) {
                        ChangePasswordResult.SUCCESS -> {
                            state = ResetPasswordStatus.SUCCESS
                        }
                        else -> {
                            state = ResetPasswordStatus.ERROR
                            errorMessage = response.message
                        }
                    }
                }
            } catch (e: Exception) {
                state = ResetPasswordStatus.ERROR
                errorMessage = "Failed to update password: ${e.message}"
            }
        }
    }

    fun resetState() {
        state = ResetPasswordStatus.IDLE
    }
}

enum class ResetPasswordStatus {
    IDLE, LOADING, SUCCESS, ERROR
}
