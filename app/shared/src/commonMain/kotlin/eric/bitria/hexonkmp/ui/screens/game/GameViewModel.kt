package eric.bitria.hexonkmp.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexonkmp.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GameRepository) : ViewModel() {
    private val _state = MutableStateFlow<GameUiState>(GameUiState.Idle)
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    fun joinGame() {
        if (_state.value != GameUiState.Idle) return
        viewModelScope.launch {
            _state.value = GameUiState.Connecting
            runCatching { repository.joinGame() }
                .onSuccess { _state.value = GameUiState.Waiting(it.gameId) }
                .onFailure { _state.value = GameUiState.Error(it.message ?: "Connection failed") }
        }
    }

    fun leaveGame() {
        // TODO: close WebSocket when implemented
        _state.value = GameUiState.Idle
    }
}
