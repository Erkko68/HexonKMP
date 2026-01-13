package eric.bitria.hexon.viewmodel.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.dtos.profile.UserProfileResponse
import eric.bitria.hexon.api.repository.ApiResult
import eric.bitria.hexon.api.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    var state by mutableStateOf<ApiResult<UserProfileResponse>>(ApiResult.Idle)
        private set

    // Mock history for now, could be fetched from repository in the future
    val gameHistory = listOf(
        GameHistoryItem(1, true, "vs. Zoe, Marcus", "2 days ago", 15),
        GameHistoryItem(2, false, "vs. Samira", "3 days ago", -10),
        GameHistoryItem(3, true, "vs. Leo", "5 days ago", 12),
        GameHistoryItem(4, true, "vs. Marcus, Zoe", "1 week ago", 18)
    )

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            state = ApiResult.Loading
            
            when (val result = userRepository.getProfile()) {
                is ApiResult.Success -> {
                    state = ApiResult.Success(result.data)
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

    fun getProcessedStats(profile: UserProfileResponse): UserStats {
        return UserStats(
            wins = profile.stats.wins.toString(),
            streak = "-",
            winRate = calculateWinRate(profile.stats.wins, profile.stats.losses)
        )
    }

    private fun calculateWinRate(wins: Int, losses: Int): String {
        val total = wins + losses
        if (total == 0) return "0%"
        val rate = (wins.toFloat() / total.toFloat()) * 100
        return "${rate.toInt()}%"
    }

    fun retry() {
        loadProfile()
    }
}
