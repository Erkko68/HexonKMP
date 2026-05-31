package eric.bitria.hexonkmp.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexonkmp.core.ws.ConnectionFailed
import eric.bitria.hexonkmp.core.ws.PlayerConnected
import eric.bitria.hexonkmp.core.ws.PlayerDisconnected
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
            is PlayerConnected -> _state.update { s ->
                when (s) {
                    // In the lobby: full room → game starts; otherwise update "X/Y".
                    is GameUiState.Waiting ->
                        if (event.connected >= event.needed) GameUiState.InGame(s.gameId)
                        else s.copy(connected = event.connected, needed = event.needed)
                    // Already playing: Catan-style, just note the (re)join.
                    is GameUiState.InGame -> s.copy(notice = "Player ${event.playerId} joined")
                    else -> s
                }
            }
            is PlayerDisconnected -> _state.update { s ->
                when (s) {
                    is GameUiState.Waiting -> s.copy(connected = event.connected, needed = event.needed)
                    // A player leaving doesn't end the game for the others.
                    is GameUiState.InGame -> s.copy(notice = "Player ${event.playerId} left")
                    else -> s
                }
            }
            is ConnectionFailed -> {
                repository.disconnect()
                _state.value = GameUiState.Error(event.reason)
            }
        }
    }
}
