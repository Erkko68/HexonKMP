package eric.bitria.hexon.viewmodel.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.render.GameCommand
import eric.bitria.hexon.render.GameEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class GameSceneViewModel: ViewModel() {

    // Technical State
    var isEngineReady by mutableStateOf(false)
        private set

    // Command Channel
    private val _gameCommands = Channel<GameCommand>(Channel.BUFFERED)
    val gameCommands = _gameCommands.receiveAsFlow()

    fun handleGameEvent(event: GameEvent) {
        when (event) {
            is GameEvent.Initialised -> {
                println("Engine Ready. Sending initial commands.")
                isEngineReady = true
            }
        }
    }

    private fun sendCommand(command: GameCommand) {
        viewModelScope.launch {
            _gameCommands.send(command)
        }
    }
}