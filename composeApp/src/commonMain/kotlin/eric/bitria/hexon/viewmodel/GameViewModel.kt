package eric.bitria.hexon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.render.GameRender
import eric.bitria.hexon.render.WebViewGameRender
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _gameRender: GameRender = WebViewGameRender()
    val gameRender: GameRender get() = _gameRender

    private val _gameEvents = MutableSharedFlow<String>()
    val gameEvents: SharedFlow<String> = _gameEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            _gameRender.receiveJson { json ->
                //println("Received from JS: $json")
                _gameEvents.emit(json)
            }
        }
    }

    fun sendCommand(command: String) {
        viewModelScope.launch {
            _gameRender.sendJson(command)
        }
    }
}