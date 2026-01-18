package eric.bitria.hexon.viewmodel.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.api.repository.ApiResult
import eric.bitria.hexon.api.repository.MatchmakingRepository
import eric.bitria.hexon.dtos.matchmaking.JoinGameResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MatchmakingViewModel(
    private val matchmakingRepository: MatchmakingRepository
) : ViewModel() {

    var playersFound by mutableStateOf(0)
        private set

    var maxPlayers by mutableStateOf(4)
        private set

    var statusMessage by mutableStateOf("Initializing...")
        private set

    private var matchmakingJob: Job? = null

    init {
        startMatchmaking()
    }

    private fun startMatchmaking() {
        matchmakingJob?.cancel()
        matchmakingJob = viewModelScope.launch {
            statusMessage = "Searching for a game..."

            when (val result = matchmakingRepository.joinGame(mode = "classic")) {
                is ApiResult.Success -> {
                    val response = result.data
                    statusMessage = if (response.status == JoinGameResult.SUCCESS) {
                        "Game found! Connecting..."
                    } else {
                        response.message
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

    fun cancelMatchmaking() {
        matchmakingJob?.cancel()
        // Logic to notify the server if necessary
    }

    override fun onCleared() {
        super.onCleared()
        matchmakingJob?.cancel()
    }
}
