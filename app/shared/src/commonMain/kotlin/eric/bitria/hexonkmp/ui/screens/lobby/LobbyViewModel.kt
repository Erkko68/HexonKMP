package eric.bitria.hexonkmp.ui.screens.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexonkmp.core.protocol.ConnectionFailed
import eric.bitria.hexonkmp.core.protocol.GameStarted
import eric.bitria.hexonkmp.core.protocol.WaitingForPlayers
import eric.bitria.hexonkmp.data.repository.GameRepository
import eric.bitria.hexonkmp.data.storage.DevicePreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Drives the lobby/main menu: choosing a name (server registration), finding a game,
// and waiting for the room to fill. It owns identity because the name dialog lives
// here; the live connection is held by the shared GameRepository, so the game screen
// picks up seamlessly once we navigate. On GameStarted it emits [gameStarted] for the
// screen to navigate — it never renders the game itself.
class LobbyViewModel(
    private val repository: GameRepository,
    private val prefs: DevicePreferences,
) : ViewModel() {
    private val _state = MutableStateFlow<LobbyUiState>(LobbyUiState.Idle)
    val state: StateFlow<LobbyUiState> = _state.asStateFlow()

    // The player's chosen display name (null until set via the name dialog). Drives
    // the prompt: null => show the dialog; present => show name + icon, enable play.
    private val _playerName = MutableStateFlow<String?>(null)
    val playerName: StateFlow<String?> = _playerName.asStateFlow()

    // One-shot signal: the game has started, navigate to the game screen.
    private val _gameStarted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val gameStarted: SharedFlow<Unit> = _gameStarted.asSharedFlow()

    // The server-issued player id, persisted locally and reused across sessions.
    private var cachedPlayerId: String? = null

    init {
        viewModelScope.launch {
            repository.events.collect { event ->
                when (event) {
                    is WaitingForPlayers -> _state.value =
                        LobbyUiState.Waiting(event.connected, event.needed, event.countdownSeconds)
                    is GameStarted<*> -> _gameStarted.tryEmit(Unit)
                    is ConnectionFailed -> _state.value =
                        LobbyUiState.Error(event.reason)
                    else -> Unit // in-game events belong to the game screen
                }
            }
        }
        viewModelScope.launch {
            cachedPlayerId = prefs.getPlayerId()
            val name = prefs.getPlayerName()
            _playerName.value = name
            // Already named from a previous session: confirm/refresh our server id up
            // front (best-effort) so joining is instant. joinGame retries if it fails.
            if (name != null) runCatching { ensureRegistered(name) }
        }
    }

    // Registers with the server — reusing our stored id if we have one (reconnection),
    // else the server mints a fresh one — then persists the issued id + name locally.
    // This is the single place the server-authoritative identity is obtained.
    private suspend fun ensureRegistered(name: String): String {
        val response = repository.register(name, cachedPlayerId)
        cachedPlayerId = response.playerId
        prefs.setPlayerId(response.playerId)
        prefs.setPlayerName(response.name)
        _playerName.value = response.name
        return response.playerId
    }

    // The name dialog submits here: store the name locally (so the UI reflects it
    // even if the server is down) and register to obtain a server id.
    fun submitName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            prefs.setPlayerName(trimmed)
            _playerName.value = trimmed
            runCatching { ensureRegistered(trimmed) } // best-effort; joinGame retries
        }
    }

    fun joinGame() {
        if (_state.value != LobbyUiState.Idle) return
        val name = _playerName.value ?: return // must choose a name first
        viewModelScope.launch {
            _state.value = LobbyUiState.Connecting
            runCatching {
                val id = cachedPlayerId ?: ensureRegistered(name)
                id to repository.joinGame(id)
            }
                .onSuccess { (id, response) ->
                    // Stay in Connecting; WaitingForPlayers moves us to Waiting and
                    // GameStarted triggers navigation to the game screen.
                    repository.connect(id, response.gameId)
                }
                .onFailure { _state.value = LobbyUiState.Error(it.message ?: "Connection failed") }
        }
    }

    // Abort matchmaking: drop the connection (the server frees our slot and cancels
    // the countdown if we were the one holding the minimum) and return to Idle.
    fun cancelSearch() {
        repository.disconnect()
        _state.value = LobbyUiState.Idle
    }

    fun retry() {
        _state.value = LobbyUiState.Idle
    }
}
