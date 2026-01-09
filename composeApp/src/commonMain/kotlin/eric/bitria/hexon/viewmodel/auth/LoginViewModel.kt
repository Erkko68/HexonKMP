package eric.bitria.hexon.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.dtos.auth.LoginRequest
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.dtos.auth.RegisterRequest
import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.client.AuthClient
import eric.bitria.hexon.client.persistence.AccountManager
import eric.bitria.hexon.utils.Validators
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class LoginViewModel(
    private val authClient: AuthClient,
    private val accountManager: AccountManager
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

    var loginState by mutableStateOf(LoginStatus.IDLE)
        private set

    var errorMessage by mutableStateOf<String?>(null)
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

    // --- Flows Triggers ---

    fun attemptAutoLogin() {
        viewModelScope.launch {
            loginState = LoginStatus.LOADING
            val success = authClient.autoLogin()
            loginState = if (success) {
                LoginStatus.SUCCESS
            } else {
                LoginStatus.IDLE
            }
        }
    }

    fun loginWithEmail() {
        if (!validateLoginForm()) return
        
        viewModelScope.launch {
            loginState = LoginStatus.LOADING
            errorMessage = null
            try {
                withTimeout(10000L) { // 10 seconds timeout
                    val response = authClient.login(LoginRequest(email, password))
                    when (response.result) {
                        LoginResult.SUCCESS -> {
                            accountManager.saveEmail(email)
                            loginState = LoginStatus.SUCCESS
                        }
                        LoginResult.NOT_VERIFIED -> {
                            loginState = LoginStatus.VERIFICATION_SENT
                        }
                        else -> {
                            loginState = LoginStatus.ERROR
                            errorMessage = response.message
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                loginState = LoginStatus.TIMEOUT
                errorMessage = "Request timed out. Please try again."
            } catch (e: Exception) {
                loginState = LoginStatus.ERROR
                errorMessage = "Login failed: ${e.message}"
            }
        }
    }

    fun registerWithEmail() {
        if (!validateRegisterForm()) return

        viewModelScope.launch {
            loginState = LoginStatus.LOADING
            errorMessage = null
            try {
                withTimeout(10000L) { // 10 seconds timeout
                    val response = authClient.register(RegisterRequest(name, email, password))
                    when (response.result) {
                        RegisterResult.SUCCESS -> {
                            accountManager.saveEmail(email)
                            loginState = LoginStatus.VERIFICATION_SENT
                        }
                        else -> {
                            loginState = LoginStatus.ERROR
                            errorMessage = response.message
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                loginState = LoginStatus.TIMEOUT
                errorMessage = "Request timed out. Please try again."
            } catch (e: Exception) {
                loginState = LoginStatus.ERROR
                errorMessage = "Registration failed: ${e.message}"
            }
        }
    }

    // Login with Google
    fun continueWithGoogle() {
        // Trigger Google login flow, then update loginState accordingly
    }

    fun resetState() {
        loginState = LoginStatus.IDLE
    }
}

// Represent the login result states
enum class LoginStatus {
    IDLE, LOADING, SUCCESS, ERROR, TIMEOUT, VERIFICATION_SENT
}
