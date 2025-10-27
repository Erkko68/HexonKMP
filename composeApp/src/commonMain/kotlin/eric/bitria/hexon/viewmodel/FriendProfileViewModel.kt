package eric.bitria.hexon.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FriendProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            username = "",
            avatarUrl = null,
            stats = UserStats("", "", ""),
            gameHistory = emptyList()
        )
    )
    val uiState = _uiState.asStateFlow()

    fun loadFriendProfile(username: String) {
        // Mock data per friend
        val mockProfiles = mapOf(
            "Zoe" to ProfileUiState(
                username = "Zoe",
                avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDIULC6J4SDUP2lmURMnmOX47KUh3wGGRkQBbsCAgnV-bTO66-WjG9RsdBd1tMMw3x1hrYB2rUycwAhUKd4-a6gPztM9THtXh3yoL901Qnm-HaYH-bAr0unh7jJo2Az--a39Ng283u4baHbk0Nq1ApVsFEF5nmGYVxS7CVa9LKFPpl1e4XljesXgREwzhdhmjDmwgNZoTVFPuKHbMf8osrASwDrLWGvCW4w5gY3zkni9BX9JqTdtqs7w4kQpwx4kb8sGQ1015VmzoM",
                stats = UserStats(wins = "102", streak = "4", winRate = "68%"),
                gameHistory = listOf(
                    GameHistoryItem(1, true, "vs. Alexi", "1 day ago", 10),
                    GameHistoryItem(2, true, "vs. Marcus, Leo", "3 days ago", 15),
                    GameHistoryItem(3, false, "vs. Samira", "5 days ago", -8)
                )
            ),
            "Marcus" to ProfileUiState(
                username = "Marcus",
                avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAwgCCe0Uw5rd6HFCr5iMhXy2JGKzZkc2tynb7Wtfc8MdbksHfDSDCB_PTTtv0hBj5o17lKflfGsOeWW1YdoccJd5Mtkv84cpyFwpYWCsJWqBQjnCwjHFhFbzcsxZzRNdqam_Z5uH6H49V-de-aP54C8fR3W7Q_2n4Kg1eyUmg9szZZVE2FEKKQvawH-Klf3BXkoTQq-TN1NpgupR1oUbTVWlqKDspZbrkwsi_1CYBYOEFAE-2HcLDAZicbHvPBq4sRyb7BDlS_dvU",
                stats = UserStats(wins = "89", streak = "2", winRate = "61%"),
                gameHistory = listOf(
                    GameHistoryItem(1, false, "vs. Alexi", "2 days ago", -12),
                    GameHistoryItem(2, true, "vs. Leo", "4 days ago", 9),
                    GameHistoryItem(3, true, "vs. Zoe", "6 days ago", 11)
                )
            ),
            "Samira" to ProfileUiState(
                username = "Samira",
                avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDauh3PIflslHSjCplf8TnCluMEf9RSpJCGsUuxcbFAmRqBp0efHL3revH5FipqYhrc2bk-vUbCRPi03T51CjzZyjjDyZVAwpc6DlcW6lFrSK1wbkmfqZYjKmgkn9Q0UUmvdGvGWe02J3eb6uOZ-nyjRuxFxXm01sMFZbvjzEyeOQAzFeqeTaTkqjm8bcMeP_odr1-Gz5wWB68dQAls05R7gEIjJWX1b8wazDiLgeb9ztJe7_h2fcxK9gG2i0CT_6hR7_waQZiABb0",
                stats = UserStats(wins = "140", streak = "10", winRate = "77%"),
                gameHistory = listOf(
                    GameHistoryItem(1, true, "vs. Zoe", "1 day ago", 20),
                    GameHistoryItem(2, true, "vs. Marcus", "3 days ago", 18),
                    GameHistoryItem(3, false, "vs. Alexi", "4 days ago", -10)
                )
            ),
            "Leo" to ProfileUiState(
                username = "Leo",
                avatarUrl = null,
                stats = UserStats(wins = "54", streak = "1", winRate = "48%"),
                gameHistory = listOf(
                    GameHistoryItem(1, false, "vs. Zoe", "2 days ago", -7),
                    GameHistoryItem(2, true, "vs. Marcus", "5 days ago", 11)
                )
            )
        )

        val profile = mockProfiles[username] ?: ProfileUiState(
            username = username,
            avatarUrl = null,
            stats = UserStats("0", "0", "0%"),
            gameHistory = emptyList()
        )

        _uiState.value = profile
    }
}
