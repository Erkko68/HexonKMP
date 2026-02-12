package eric.bitria.hexon.viewmodel.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.data.repository.ApiResult
import eric.bitria.hexon.data.repository.GameRepository
import eric.bitria.hexon.data.repository.MatchmakingRepository
import eric.bitria.hexon.dtos.matchmaking.JoinGameResult
import eric.bitria.hexon.ws.LobbyEvent
import eric.bitria.hexon.ws.LobbyIntent
import eric.bitria.hexon.ws.lobby.GameMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MatchmakingViewModel(
    private val matchmakingRepository: MatchmakingRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    // --- UI State ---
    private val _navigateToGameplay = MutableStateFlow(false)
    val navigateToGameplay = _navigateToGameplay.asStateFlow()

    var playersFound by mutableStateOf(0)
        private set

    var maxPlayers by mutableStateOf(4)
        private set

    var statusMessage by mutableStateOf("Initializing...")
        private set

    private var matchmakingJob: Job? = null
    private var messageListenerJob: Job? = null

    init {
        // Listen to lobby messages
        messageListenerJob = viewModelScope.launch {
            gameRepository.incomingMessages.collect { message ->
                // Only handle lobby messages
                if (message is LobbyEvent) {
                    handleLobbyMessage(message)
                }
                // Ignore other messages - GameViewModel will handle them
            }
        }

        // Begin the matchmaking process
        startMatchmaking()
    }

    private fun startMatchmaking() {
        matchmakingJob = viewModelScope.launch {
            statusMessage = "Searching for a game..."

            when (val result = matchmakingRepository.joinGame(mode = GameMode.CLASSIC)) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.status == JoinGameResult.SUCCESS && response.sessionId != null) {
                        statusMessage = "Game found! Connecting..."

                        // Delegate connection to the Repository (Singleton).
                        // This connection will persist after this ViewModel is destroyed.
                        try {
                            gameRepository.connect(response.sessionId!!)
                        } catch (e: Exception) {
                            statusMessage = "Connection failed: ${e.message}"
                        }
                    } else {
                        statusMessage = response.message
                    }
                }
                is ApiResult.Error -> {
                    statusMessage = result.message ?: "Failed to join matchmaking"
                }
                ApiResult.NetworkError -> {
                    statusMessage = "Network error. Please check your connection."
                }
                else -> {}
            }
        }
    }

    private fun handleLobbyMessage(message: LobbyEvent) {
        when (message) {
            is LobbyEvent.LobbySnapshot -> {
                playersFound = message.lobbyPlayers.size
                maxPlayers = message.maxPlayers
                statusMessage = "Waiting for players..."
            }
            is LobbyEvent.PlayerJoined -> {
                playersFound++
            }
            is LobbyEvent.PlayerLeft -> {
                playersFound--
            }
            is LobbyEvent.GameStarted -> {
                statusMessage = "Starting Game..."

                // Signal to server that we're ready to receive game initialization
                viewModelScope.launch {
                    gameRepository.sendMessage(LobbyIntent.ReadyForGame)
                }

                // Stop listening to messages - GameViewModel will take over
                messageListenerJob?.cancel()

                // Trigger navigation
                _navigateToGameplay.value = true
            }
            is LobbyEvent.LobbyError -> {
                statusMessage = message.errorMessage
            }
            is LobbyEvent.PlayerUpdated -> {
                // Ignore in matchmaking mode
            }
        }
    }

    /**
     * Called when the user manually cancels matchmaking (e.g. Back button).
     * Only HERE do we explicitly disconnect, because the user is aborting the flow.
     */
    fun leaveMatchmaking() {
        viewModelScope.launch {
            try {
                // Ideally, tell the server we are leaving
                gameRepository.sendMessage(LobbyIntent.LeaveLobby)

                // Clean up the connection since we are quitting
                gameRepository.disconnect()
            } catch (e: Exception) {
                // Ignore errors during departure
            } finally {
                matchmakingJob?.cancel()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        matchmakingJob?.cancel()
    }
}