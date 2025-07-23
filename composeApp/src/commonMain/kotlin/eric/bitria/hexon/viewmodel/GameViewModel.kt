package eric.bitria.hexon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.communication.GameCommunication
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _gameCommunication = GameCommunication()
    val gameCommunication: GameCommunication get() = _gameCommunication

    val gameEvents: SharedFlow<String> = _gameCommunication.gameEvents

    init {
        viewModelScope.launch {
            _gameCommunication.gameEvents.collect { json ->
                // Handle game events if needed, or just expose the flow directly
            }
        }
    }

    fun sendCommand(command: String) {
        viewModelScope.launch {
            _gameCommunication.sendJson(command)
        }
    }
}