package eric.bitria.hexon.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.api.SessionManager
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.api.repository.ApiResult
import eric.bitria.hexon.api.repository.AuthRepository
import eric.bitria.hexon.api.repository.UserRepository
import eric.bitria.hexon.utils.Validators
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // --- Fields ---
    var name by mutableStateOf("")
        private set

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    var loginState by mutableStateOf<ApiResult<LoginResult>>(ApiResult.Idle)
        private set

    var nameError by mutableStateOf<String?>(null)
        private set

    var emailError by mutableStateOf<String?>(null)
        private set

    var passwordError by mutableStateOf<String?>(null)
        private set

    var confirmPasswordError by mutableStateOf<String?>(null)
        private set

    // --- Field Triggers ---
    fun onNameChange(newName: String) {
        name = newName
        nameError = if (Validators.isValidUsername(newName)) null 
                   else "Username must be 3-20 characters and contain only letters, numbers, and underscores."
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

    fun onEmailChange(newEmail: String) {
        email = newEmail
        emailError = if (Validators.isValidEmail(newEmail)) null else "Invalid email format."
    }

    private fun validateLoginForm(): Boolean {
        onEmailChange(email)
        onPasswordChange(password)
        return emailError == null && passwordError == null
    }

    private fun validateRegisterForm(): Boolean {
        onNameChange(name)
        onEmailChange(email)
        onPasswordChange(password)
        validateConfirmPassword()
        return nameError == null && emailError == null && passwordError == null && confirmPasswordError == null
    }

    fun loginWithEmail() {
        if (!validateLoginForm()) return

        viewModelScope.launch {
            loginState = ApiResult.Loading

            when (val result = authRepository.login(email, password)) {
                is ApiResult.Success -> {
                    loginState = when (result.data) {
                        LoginResult.SUCCESS -> {
                            sessionManager.login()
                            ApiResult.Success(LoginResult.SUCCESS)
                        }
                        LoginResult.NOT_VERIFIED -> {
                            userRepository.resendVerificationCode(email)
                            ApiResult.Success(LoginResult.NOT_VERIFIED)
                        }
                        LoginResult.INVALID_CREDENTIALS -> {
                            ApiResult.Error("Invalid email or password.")
                        }
                        else -> {
                            ApiResult.Error("An unexpected error occurred.")
                        }
                    }
                }
                is ApiResult.NetworkError -> {
                    loginState = ApiResult.NetworkError
                }
                is ApiResult.Error -> {
                    loginState = ApiResult.Error(result.message ?: "Login failed.")
                }
                else -> {}
            }
        }
    }

    fun registerWithEmail() {
        if (!validateRegisterForm()) return

        viewModelScope.launch {
            loginState = ApiResult.Loading

            when (val result = authRepository.register(email, name, password)) {
                is ApiResult.Success -> {
                    loginState = when (result.data) {
                        RegisterResult.SUCCESS -> {
                            ApiResult.Success(LoginResult.NOT_VERIFIED)
                        }
                        RegisterResult.EMAIL_ALREADY_EXISTS -> {
                            ApiResult.Error("Email already in use.")
                        }
                        RegisterResult.USERNAME_ALREADY_EXISTS -> {
                            ApiResult.Error("Username already taken.")
                        }
                        else -> {
                            ApiResult.Error("An unexpected error occurred.")
                        }
                    }
                }
                is ApiResult.NetworkError -> {
                    loginState = ApiResult.NetworkError
                }
                is ApiResult.Error -> {
                    loginState = ApiResult.Error(result.message ?: "Registration failed.")
                }
                else -> {}
            }
        }
    }

    // Login with Google
    fun continueWithGoogle() {
        // Trigger Google login flow, then update loginState accordingly
    }

    fun resetState() {
        loginState = ApiResult.Idle
    }
}
