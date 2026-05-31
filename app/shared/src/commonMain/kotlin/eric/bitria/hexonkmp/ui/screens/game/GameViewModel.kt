package eric.bitria.hexonkmp.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexonkmp.core.ws.ConnectionFailed
import eric.bitria.hexonkmp.core.ws.GameStarted
import eric.bitria.hexonkmp.core.ws.PlayerDisconnected
import eric.bitria.hexonkmp.core.ws.WaitingForPlayers
import eric.bitria.hexonkmp.data.repository.GameRepository
import eric.bitria.hexonkmp.data.storage.DevicePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: GameRepository,
    private val prefs: DevicePreferences,
) : ViewModel() {
    private val _state = MutableStateFlow<GameUiState>(GameUiState.Idle)
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.events.collect { handleServerEvent(it) }
        }
    }

    fun joinGame() {
        if (_state.value != GameUiState.Idle) return
        viewModelScope.launch {
            _state.value = GameUiState.Connecting
            runCatching {
                val playerId = prefs.getOrCreatePlayerId()
                playerId to repository.joinGame(playerId)
            }
                .onSuccess { (playerId, response) ->
                    _state.value = GameUiState.Waiting(response.gameId)
                    repository.connect(playerId, response.gameId)
                }
                .onFailure { _state.value = GameUiState.Error(it.message ?: "Connection failed") }
        }
    }

    fun retryJoinGame() {
        _state.value = GameUiState.Idle
        joinGame()
    }

    fun leaveGame() {
        repository.disconnect()
        _state.value = GameUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }

    private fun handleServerEvent(event: eric.bitria.hexonkmp.core.ws.ServerEvent) {
        when (event) {
            is WaitingForPlayers -> _state.update { s ->
                if (s is GameUiState.Waiting) s.copy(connected = event.connected, needed = event.needed)
                else s
            }
            GameStarted -> {
                val gameId = (_state.value as? GameUiState.Waiting)?.gameId ?: return
                _state.value = GameUiState.InGame(gameId)
            }
            PlayerDisconnected -> leaveGame()
            is ConnectionFailed -> {
                repository.disconnect()
                _state.value = GameUiState.Error(event.reason)
            }
        }
    }
}
