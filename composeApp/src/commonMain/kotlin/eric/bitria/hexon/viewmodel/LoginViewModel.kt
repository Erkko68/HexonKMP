package eric.bitria.hexon.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginViewModel(
    //private val authRepository: AuthRepository
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

    var loginState by mutableStateOf<LoginState>(LoginState.Idle)
        private set

    // --- Field Triggers ---
    fun onNameChange(newName: String) {
        name = newName
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    fun onConfirmPasswordChange(newConfirmPassword: String) {
        confirmPassword = newConfirmPassword
    }

    fun onEmailChange(newEmail: String) {
        email = newEmail
    }

    // --- Flows Triggers ---

    fun loginWithEmail() {
        viewModelScope.launch {
            loginState = LoginState.Loading
            loginState = try {
                val success = true
                delay(0)
                if (success) LoginState.Success else LoginState.Error("Invalid email")
            } catch (e: Exception) {
                LoginState.Error("Login failed: ${e.message}")
            }
        }
    }

    fun registerWithEmail(){
        viewModelScope.launch {
            loginState = LoginState.Loading
            loginState = try {
                val success = true
                delay(0)
                if (success) LoginState.Success else LoginState.Error("Invalid email")
            } catch (e: Exception) {
                LoginState.Error("Login failed: ${e.message}")
            }
        }
    }

    // Login with Google
    fun continueWithGoogle() {
        // Trigger Google login flow, then update loginState accordingly
        // You can expose a callback or handle this externally if using Google SDK
    }
}

// Represent the login result states
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
