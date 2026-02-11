package eric.bitria.hexon.viewmodel.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.data.repository.ApiResult
import eric.bitria.hexon.data.repository.UserRepository
import eric.bitria.hexon.dtos.profile.PublicUserProfileResponse
import kotlinx.coroutines.launch

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
    private val userRepository: UserRepository
) : ViewModel() {

    var state by mutableStateOf<ApiResult<PublicUserProfileResponse>>(ApiResult.Idle)
        private set

    fun loadFriendProfile(userId: String) {
        viewModelScope.launch {
            state = ApiResult.Loading
            
            when (val result = userRepository.getPublicProfile(userId)) {
                is ApiResult.Success -> {
                    val profile = result.data
                    state = if (profile != null) {
                        ApiResult.Success(profile)
                    } else {
                        ApiResult.Error("User not found")
                    }
                }
                is ApiResult.NetworkError -> {
                    state = ApiResult.NetworkError
                }
                is ApiResult.Error -> {
                    state = ApiResult.Error(result.message ?: "Failed to load profile")
                }
                else -> {}
            }
        }
    }

    /**
     * Helper to transform the DTO stats into UI-friendly UserStats.
     * This can be called by the UI when the state is [ApiResult.Success].
     */
    fun getProcessedStats(profile: PublicUserProfileResponse): UserStats {
        return UserStats(
            wins = profile.stats.wins.toString(),
            streak = "-", // Need backend support for streak
            winRate = calculateWinRate(profile.stats.wins, profile.stats.losses)
        )
    }

    private fun calculateWinRate(wins: Int, losses: Int): String {
        val total = wins + losses
        if (total == 0) return "0%"
        val rate = (wins.toFloat() / total.toFloat()) * 100
        return "${rate.toInt()}%"
    }

    fun resetState() {
        state = ApiResult.Idle
    }
}
