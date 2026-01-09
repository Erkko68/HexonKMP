package eric.bitria.hexon.viewmodel.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.client.repository.UserClient
import eric.bitria.hexon.dtos.account.ChangePasswordRequest
import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.utils.Validators
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ForgotPasswordViewModel(
    private val userClient: UserClient
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var resetCode by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    var state by mutableStateOf(ResetPasswordStatus.IDLE)
        private set

    var errorMessage by mutableStateOf<String?>(null)
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
            state = ResetPasswordStatus.LOADING
            errorMessage = null
            try {
                withTimeout(10000L) {
                    val response = userClient.changePassword(
                        ChangePasswordRequest(
                            email = email,
                            resetCode = resetCode,
                            oldPassword = null,
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
                errorMessage = "Failed to reset password: ${e.message}"
            }
        }
    }

    fun resetState() {
        state = ResetPasswordStatus.IDLE
    }
}
