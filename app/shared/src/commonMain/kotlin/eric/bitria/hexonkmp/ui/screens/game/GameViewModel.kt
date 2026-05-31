package eric.bitria.hexonkmp.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.event.TurnChanged
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.protocol.ActionRejected
import eric.bitria.hexonkmp.core.protocol.ConnectionFailed
import eric.bitria.hexonkmp.core.protocol.GameStarted
import eric.bitria.hexonkmp.core.protocol.GameUpdate
import eric.bitria.hexonkmp.core.protocol.PlayerJoined
import eric.bitria.hexonkmp.core.protocol.PlayerLeft
import eric.bitria.hexonkmp.core.protocol.ServerEvent
import eric.bitria.hexonkmp.core.protocol.WaitingForPlayers
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

    private var myPlayerId: PlayerId? = null

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
                    myPlayerId = PlayerId(playerId)
                    _state.value = GameUiState.Waiting(response.gameId)
                    repository.connect(playerId, response.gameId)
                }
                .onFailure { _state.value = GameUiState.Error(it.message ?: "Connection failed") }
        }
    }

    fun endTurn() {
        val s = _state.value
        if (s is GameUiState.InGame && s.isMyTurn) repository.sendAction(EndTurn)
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

    private fun handleServerEvent(event: ServerEvent) {
        when (event) {
            // --- Lobby phase ---
            is WaitingForPlayers -> _state.update { s ->
                if (s is GameUiState.Waiting) s.copy(connected = event.connected, needed = event.needed)
                else s
            }
            is GameStarted -> _state.update { s ->
                // Fired when the room fills, or on reconnect into a running game.
                val me = myPlayerId
                if (s is GameUiState.Waiting && me != null) {
                    GameUiState.InGame(gameId = s.gameId, state = event.state, myPlayerId = me)
                } else s
            }
            // --- Presence: Catan-style, players coming and going don't end the
            // game for the others — just surface a notice. ---
            is PlayerJoined -> _state.update { s ->
                if (s is GameUiState.InGame) s.copy(notice = "Player ${event.playerId} joined") else s
            }
            is PlayerLeft -> _state.update { s ->
                if (s is GameUiState.InGame) s.copy(notice = "Player ${event.playerId} left") else s
            }
            // --- Game updates: apply the domain event to the local state copy. ---
            is GameUpdate -> _state.update { s ->
                if (s is GameUiState.InGame) s.copy(state = applyEvent(s, event)) else s
            }
            // --- Action feedback (only the acting player receives this) ---
            is ActionRejected -> _state.update { s ->
                if (s is GameUiState.InGame) s.copy(notice = event.reason) else s
            }
            // --- Client-local ---
            is ConnectionFailed -> {
                repository.disconnect()
                _state.value = GameUiState.Error(event.reason)
            }
        }
    }

    private fun applyEvent(s: GameUiState.InGame, update: GameUpdate) =
        when (val e = update.event) {
            is TurnChanged -> s.state.copy(
                currentPlayerIndex = s.state.players.indexOf(e.currentPlayer),
                turn = e.turn,
            )
        }
}
