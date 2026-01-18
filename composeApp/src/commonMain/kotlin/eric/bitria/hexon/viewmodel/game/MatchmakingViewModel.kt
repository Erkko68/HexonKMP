package eric.bitria.hexon.viewmodel.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.api.repository.ApiResult
import eric.bitria.hexon.api.repository.GameRepository
import eric.bitria.hexon.api.repository.MatchmakingRepository
import eric.bitria.hexon.dtos.matchmaking.JoinGameResult
import eric.bitria.hexon.ws.LobbyEvent
import eric.bitria.hexon.ws.LobbyIntent
import eric.bitria.hexon.ws.data.GameMode
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MatchmakingViewModel(
    private val matchmakingRepository: MatchmakingRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    var navigateToGameplay by mutableStateOf(false)
        private set

    var playersFound by mutableStateOf(0)
        private set

    var maxPlayers by mutableStateOf(4)
        private set

    var statusMessage by mutableStateOf("Initializing...")
        private set

    private var matchmakingJob: Job? = null
    private var socketSession: DefaultWebSocketSession? = null

    init {
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
                        connectToSocket(response.sessionId!!)
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

    private suspend fun connectToSocket(sessionId: String) {
        try {
            val session = gameRepository.connect(sessionId)
            socketSession = session

            gameRepository.observeMessages(session).collect { message ->
                when (message) {
                    is LobbyEvent.LobbySnapshot -> {
                        playersFound = message.players.size
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
                        navigateToGameplay = true
                    }
                    is LobbyEvent.LobbyError -> {
                        statusMessage = message.errorMessage
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            statusMessage = "Connection lost: ${e.message}"
        } finally {
            socketSession?.close()
            socketSession = null
        }
    }

    /**
     * Called when the user manually cancels matchmaking.
     * We send a leave intent to the server and the navigation will handle
     * clearing the ViewModel via onCleared.
     */
    fun leaveMatchmaking() {
        viewModelScope.launch {
            try {
                socketSession?.let { session ->
                    gameRepository.sendMessage(session, LobbyIntent.LeaveLobby())
                }
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
        socketSession = null
    }
}