package eric.bitria.hexon.viewmodel.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.api.client.UserClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val id: String = "",
    val username: String = "",
    val stats: UserStats = UserStats("", "", ""),
    val gameHistory: List<GameHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class UserStats(
    val wins: String,
    val streak: String,
    val winRate: String
)

data class GameHistoryItem(
    val id: Int,
    val won: Boolean,
    val opponent: String,
    val date: String,
    val points: Int
)

class FriendProfileViewModel(
    private val userClient: UserClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    fun loadFriendProfile(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val profile = userClient.getPublicProfile(userId)
                
                if (profile != null) {
                    _uiState.update { 
                        it.copy(
                            id = profile.id,
                            username = profile.username,
                            stats = UserStats(
                                wins = profile.stats.wins.toString(),
                                streak = "-", // Need backend support for streak
                                winRate = calculateWinRate(profile.stats.wins, profile.stats.losses)
                            ),
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = "User not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Failed to load profile"
                    )
                }
            }
        }
    }

    private fun calculateWinRate(wins: Int, losses: Int): String {
        val total = wins + losses
        if (total == 0) return "0%"
        val rate = (wins.toFloat() / total.toFloat()) * 100
        return "${rate.toInt()}%"
    }
}
