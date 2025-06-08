package eric.bitria.hexon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.screen.GameScreen
import eric.bitria.hexon.screen.WebViewGameScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _gameScreen: GameScreen = WebViewGameScreen()
    val gameScreen: GameScreen get() = _gameScreen

    private val _gameEvents = MutableSharedFlow<String>()
    val gameEvents: SharedFlow<String> = _gameEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            _gameScreen.receiveJson { json ->
                _gameEvents.emit(json)
            }
        }
    }

    fun sendCommand(command: String) {
        viewModelScope.launch {
            _gameScreen.sendJson(command)
        }
    }
}