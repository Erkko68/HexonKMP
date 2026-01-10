package eric.bitria.hexon.viewmodel.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.client.UserClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userClient: UserClient
) : ViewModel() {

    private val sampleHistory = listOf(
        GameHistoryItem(1, true, "vs. Zoe, Marcus", "2 days ago", 15),
        GameHistoryItem(2, false, "vs. Samira", "3 days ago", -10),
        GameHistoryItem(3, true, "vs. Leo", "5 days ago", 12),
        GameHistoryItem(4, true, "vs. Marcus, Zoe", "1 week ago", 18)
    )

    private val _uiState = MutableStateFlow(ProfileUiState(gameHistory = sampleHistory))
    val uiState = _uiState.asStateFlow()

    init {
        fetchProfile()
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            try {
                val response = userClient.getMe()
                _uiState.update { state ->
                    state.copy(
                        username = response.username,
                        avatarUrl = null, // Backend doesn't provide avatarUrl yet
                        stats = UserStats(
                            wins = response.stats.wins.toString(),
                            streak = "-", // Streak not available in getMe yet
                            winRate = "${(response.stats.winRate * 100).toInt()}%"
                        ),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        fetchProfile()
    }
}

data class GameHistoryItem(
    val id: Int,
    val isWin: Boolean,
    val opponents: String,
    val date: String,
    val lpChange: Int
)

data class UserStats(
    val wins: String,
    val streak: String,
    val winRate: String
)

data class ProfileUiState(
    val username: String = "",
    val avatarUrl: String? = null,
    val stats: UserStats = UserStats("0", "-", "0%"),
    val gameHistory: List<GameHistoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)