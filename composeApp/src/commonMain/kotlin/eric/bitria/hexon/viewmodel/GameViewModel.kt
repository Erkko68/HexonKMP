package eric.bitria.hexon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    // Debug Flow
    private val _gameEvents = MutableSharedFlow<String>()
    val gameEvents: SharedFlow<String> = _gameEvents.asSharedFlow()

    private var sendJsonHandler: (suspend (String) -> Unit)? = null

    fun setSendJsonHandler(handler: suspend (String) -> Unit) {
        sendJsonHandler = handler
    }

    fun sendCommand(command: String) {
        viewModelScope.launch {
            sendJsonHandler?.invoke(command)
        }
    }

    fun handleReceivedJson(json: String) {
        viewModelScope.launch {
            _gameEvents.emit(json)
        }
    }
}