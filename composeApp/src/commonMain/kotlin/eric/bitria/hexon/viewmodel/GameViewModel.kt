package eric.bitria.hexon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _gameEvents = MutableSharedFlow<String>()
    val gameEvents: SharedFlow<String> = _gameEvents.asSharedFlow()

    private val _sendJson = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val sendJson: SharedFlow<String> = _sendJson.asSharedFlow()

    fun sendCommand(command: String) {
        viewModelScope.launch {
            _sendJson.emit(command)
        }
    }

    fun onJsonReceived(json: String) {
        viewModelScope.launch {
            _gameEvents.emit(json)
        }
    }
}