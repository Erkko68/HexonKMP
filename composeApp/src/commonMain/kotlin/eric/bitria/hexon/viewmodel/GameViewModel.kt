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

    fun testCommand() {
        viewModelScope.launch {
            _sendJson.emit(
"""{
          "type": "INIT_BOARD",
          "config": {
            "radius": 2,
            "tiles": [
              {
                "type": "forest",
                "position": { "q": 0, "r": 0 },
                "token": 5
              },
              {
                "type": "hills",
                "position": { "q": 1, "r": 0 },
                "token": 2
              },
              {
                "type": "pasture",
                "position": { "q": 0, "r": -1 },
                "token": 10
              },
              {
                "type": "desert",
                "position": { "q": -1, "r": 0 },
                "token": null
              },
              {
                "type": "fields",
                "position": { "q": -1, "r": 1 },
                "token": 3
              },
              {
                "type": "mountains",
                "position": { "q": 1, "r": -1 },
                "token": 6
              },
              {
                "type": "fields",
                "position": { "q": 0, "r": 1 },
                "token": 8
              }
            ]
          }
    }""".trimIndent())
        }
    }
}