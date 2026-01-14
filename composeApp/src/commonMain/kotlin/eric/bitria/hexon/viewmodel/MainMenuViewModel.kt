package eric.bitria.hexon.viewmodel

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

class MainMenuViewModel : ViewModel() {

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

                // Send setup commands to JS
                //sendCommand(GameCommand.UpdateSpeed(0.05f))
            }
            is GameEvent.ObjectClicked -> {
                println("Clicked: ${event.id}")
            }
            is GameEvent.AnimationFinished -> {
                // Handle animation finish
            }
        }
    }

    private fun sendCommand(command: GameCommand) {
        viewModelScope.launch {
            _gameCommands.send(command)
        }
    }
}