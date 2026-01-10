package eric.bitria.hexon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.client.auth.SessionManager
import kotlinx.coroutines.launch

class AppViewModel: ViewModel() {
    val sessionState = SessionManager.sessionState

    init {
        viewModelScope.launch {
            SessionManager.initSession()
        }
    }
}