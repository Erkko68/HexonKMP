package eric.bitria.hexonkmp.ui.screens.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexonkmp.core.protocol.ConnectionFailed
import eric.bitria.hexonkmp.core.protocol.GameStarted
import eric.bitria.hexonkmp.core.protocol.LobbyRoster
import eric.bitria.hexonkmp.data.repository.GameRepository
import eric.bitria.hexonkmp.data.storage.DevicePreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Drives the main menu AND the waiting lobby (two screens, one shared instance — the
// connection, identity, and roster all live here). Matchmaking and private lobbies
// share the room (LobbyUiState.InLobby), fed by the server's LobbyRoster. Navigation
// is signalled, not pushed: [enterLobby] fires once a connection is established (menu
// -> lobby), [gameStarted] fires when the host/countdown starts the game (lobby ->
// game). The live connection is held by the shared GameRepository.
class LobbyViewModel(
    private val repository: GameRepository,
    private val prefs: DevicePreferences,
) : ViewModel() {
    private val _state = MutableStateFlow<LobbyUiState>(LobbyUiState.Idle)
    val state: StateFlow<LobbyUiState> = _state.asStateFlow()

    // The player's chosen display name (null until set via the name dialog).
    private val _playerName = MutableStateFlow<String?>(null)
    val playerName: StateFlow<String?> = _playerName.asStateFlow()

    // Inline error for the private-lobby dialog (e.g. a bad code), separate from the
    // full-screen Error state so the dialog can show it without tearing down the menu.
    private val _joinError = MutableStateFlow<String?>(null)
    val joinError: StateFlow<String?> = _joinError.asStateFlow()

    // Navigation signals (collected by the screens).
    private val _enterLobby = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val enterLobby: SharedFlow<Unit> = _enterLobby.asSharedFlow()
    private val _gameStarted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val gameStarted: SharedFlow<Unit> = _gameStarted.asSharedFlow()

    // Our identity: the secret auth token (the credential) and the playerId it maps to.
    private var cachedToken: String? = null
    private var cachedPlayerId: String? = null
    // Join code of the private lobby we're in (null for matchmaking); shown in the room.
    private var lobbyCode: String? = null

    init {
        viewModelScope.launch {
            repository.events.collect { event ->
                when (event) {
                    is LobbyRoster -> {
                        val me = cachedPlayerId
                        val isHost = me != null && me == event.hostId
                        _state.value = LobbyUiState.InLobby(
                            members = event.members,
                            hostId = event.hostId,
                            isHost = isHost,
                            canStart = isHost && event.members.size >= event.minPlayers,
                            maxPlayers = event.maxPlayers,
                            code = lobbyCode,
                            countdownSeconds = event.countdownSeconds,
                        )
                    }
                    is GameStarted<*> -> _gameStarted.tryEmit(Unit)
                    is ConnectionFailed -> _state.value = LobbyUiState.Error(event.reason)
                    else -> Unit // in-game events belong to the game screen
                }
            }
        }
        viewModelScope.launch {
            cachedToken = prefs.getToken()
            cachedPlayerId = prefs.getPlayerId()
            val name = prefs.getPlayerName()
            _playerName.value = name
            // Already named from a previous session: confirm/refresh our identity up
            // front (best-effort) so playing is instant. The actions below retry if it failed.
            if (name != null) runCatching { ensureRegistered(name) }
        }
    }

    // Registers with the server — presenting our stored token to reclaim the same
    // identity, or minting a fresh one — and persists the issued token + playerId + name.
    private suspend fun ensureRegistered(name: String): String {
        val response = repository.register(name, cachedToken)
        cachedToken = response.token
        cachedPlayerId = response.playerId
        prefs.setToken(response.token)
        prefs.setPlayerId(response.playerId)
        prefs.setPlayerName(response.name)
        _playerName.value = response.name
        return response.playerId
    }

    fun submitName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            prefs.setPlayerName(trimmed)
            _playerName.value = trimmed
            runCatching { ensureRegistered(trimmed) } // best-effort; the play actions retry
        }
    }

    // --- Entering a lobby (matchmaking or private) ---

    fun findGame() = enter(onError = { _state.value = LobbyUiState.Error(it) }) {
        lobbyCode = null
        repository.joinGame().gameId
    }

    fun createLobby() = enter(onError = { _state.value = LobbyUiState.Error(it) }) {
        val response = repository.createLobby()
        lobbyCode = response.code
        response.gameId
    }

    fun joinLobby(code: String) {
        val digits = code.filter { it.isDigit() }
        if (digits.length != 6) {
            _joinError.value = "Enter a 6-digit code"
            return
        }
        enter(onError = { _joinError.value = "Lobby not found" }) {
            val response = repository.joinLobby(digits)
            lobbyCode = digits
            response.gameId
        }
    }

    // Ensures we have an identity, runs [block] (the HTTP that returns a gameId — the
    // Auth plugin attaches our token and transparently re-registers + retries on a 401),
    // then opens the WS and signals navigation. Only navigates on success, so a failed
    // join surfaces via [onError] without leaving the menu. Identity is re-read from
    // storage afterwards in case the plugin refreshed it mid-call.
    private fun enter(
        onError: (String) -> Unit,
        block: suspend () -> String,
    ) {
        val name = _playerName.value ?: return // must choose a name first
        _joinError.value = null
        viewModelScope.launch {
            runCatching {
                if (cachedToken == null) ensureRegistered(name)
                val gameId = block()
                cachedToken = prefs.getToken()
                cachedPlayerId = prefs.getPlayerId()
                val token = cachedToken ?: error("Not registered")
                val playerId = cachedPlayerId ?: error("Not registered")
                Triple(token, playerId, gameId)
            }.onSuccess { (token, playerId, gameId) ->
                _state.value = LobbyUiState.Connecting
                repository.connect(token, playerId, name, gameId)
                _enterLobby.tryEmit(Unit)
            }.onFailure { onError(it.message ?: "Connection failed") }
        }
    }

    fun clearJoinError() {
        _joinError.value = null
    }

    // Host-only: ask the server to start. The server broadcasts GameStarted, which
    // flows back as the [gameStarted] navigation signal.
    fun startGame() {
        val gameId = repository.currentGameId ?: return
        viewModelScope.launch { runCatching { repository.startLobby(gameId) } }
    }

    // Leave the lobby (cancel matchmaking or exit a private lobby): drop the
    // connection — the server frees our slot, cancels any countdown, and promotes a
    // new host if we were one — and reset to the menu.
    fun leave() {
        repository.disconnect()
        lobbyCode = null
        _state.value = LobbyUiState.Idle
    }
}
