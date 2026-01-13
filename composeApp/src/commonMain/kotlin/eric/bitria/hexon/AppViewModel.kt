package eric.bitria.hexon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.api.SessionManager
import kotlinx.coroutines.launch

class AppViewModel(
    private val sessionManager: SessionManager
): ViewModel() {
    val sessionState = sessionManager.sessionState

    init {
        viewModelScope.launch {
            sessionManager.initSession()
        }
    }
}
