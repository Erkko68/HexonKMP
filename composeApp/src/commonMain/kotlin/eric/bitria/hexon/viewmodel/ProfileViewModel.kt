package eric.bitria.hexon.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel : ViewModel() {
    private val sampleStats = UserStats(
        wins = "128",
        streak = "7",
        winRate = "72%"
    )

    private val sampleHistory = listOf(
        GameHistoryItem(1, true, "vs. Zoe, Marcus", "2 days ago", 15),
        GameHistoryItem(2, false, "vs. Samira", "3 days ago", -10),
        GameHistoryItem(3, true, "vs. Leo", "5 days ago", 12),
        GameHistoryItem(4, true, "vs. Marcus, Zoe", "1 week ago", 18)
    )

    private val initialState = ProfileUiState(
        username = "Alexi",
        avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuC51H-gMNn1Saq065BCzCz7XsqWFREXkvi1xyGWbypQ8_Phf363LDOL0aHUf3KpdnKzf0Mjh9UTNYoJulg5EeEGjZOmCWo84VA1grdl2AMF6j97vYkyyF7vJ6T3o8yUfEPgYIZ40ruu2S0PRNrVpR5IBsgkeBD0Wae02QU79FCZoglqNAsj6hBpsBg8CQTvDO1FTzwyT_9jIXjA8HANwa4pNqod3CW-H35vU-as9HXnMX6OfuUyOmelWs7yzHg_um5Ije3fMBFXD0c",
        stats = sampleStats,
        gameHistory = sampleHistory
    )

    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()
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
    val username: String,
    val avatarUrl: String?,
    val stats: UserStats,
    val gameHistory: List<GameHistoryItem>
)