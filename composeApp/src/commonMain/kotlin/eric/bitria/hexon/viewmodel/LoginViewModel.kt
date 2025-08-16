package eric.bitria.hexon.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LoginViewModel(
    //private val authRepository: AuthRepository
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var loginState by mutableStateOf<LoginState>(LoginState.Idle)
        private set

    // Called when email input changes
    fun onEmailChange(newEmail: String) {
        email = newEmail
    }

    // Call backend to login with email
    fun loginWithEmail() {
        viewModelScope.launch {
            loginState = LoginState.Loading
            loginState = try {
                val success = true
                if (success) LoginState.Success else LoginState.Error("Invalid email")
            } catch (e: Exception) {
                LoginState.Error("Login failed: ${e.message}")
            }
        }
    }

    // Login with Google
    fun loginWithGoogle() {
        // Trigger Google login flow, then update loginState accordingly
        // You can expose a callback or handle this externally if using Google SDK
    }

    // Login with Apple ID
    fun loginWithApple() {
        // Same idea as Google login
    }
}

// Represent the login result states
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
