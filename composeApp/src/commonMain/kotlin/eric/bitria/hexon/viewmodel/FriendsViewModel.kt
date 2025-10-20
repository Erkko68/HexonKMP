package eric.bitria.hexon.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 1. Data Class for a Friend
data class Friend(
    val id: Int,
    val username: String,
    val isOnline: Boolean,
    val avatarUrl: String? = null
)

class FriendsViewModel : ViewModel() {
    private val _friendsList = MutableStateFlow<List<Friend>>(emptyList())

    val friendsList: StateFlow<List<Friend>> = _friendsList.asStateFlow()

    init {
        val sampleData = listOf(
            Friend(id = 1, username = "Alexi", isOnline = true, avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuC51H-gMNn1Saq065BCzCz7XsqWFREXkvi1xyGWbypQ8_Phf363LDOL0aHUf3KpdnKzf0Mjh9UTNYoJulg5EeEGjZOmCWo84VA1grdl2AMF6j97vYkyyF7vJ6T3o8yUfEPgYIZ40ruu2S0PRNrVpR5IBsgkeBD0Wae02QU79FCZoglqNAsj6hBpsBg8CQTvDO1FTzwyT_9jIXjA8HANwa4pNqod3CW-H35vU-as9HXnMX6OfuUyOmelWs7yzHg_um5Ije3fMBFXD0c"),
            Friend(id = 2, username = "Zoe", isOnline = false, avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDIULC6J4SDUP2lmURMnmOX47KUh3wGGRkQBbsCAgnV-bTO66-WjG9RsdBd1tMMw3x1hrYB2rUycwAhUKd4-a6gPztM9THtXh3yoL901Qnm-HaYH-bAr0unh7jJo2Az--a39Ng283u4baHbk0Nq1ApVsFEF5nmGYVxS7CVa9LKFPpl1e4XljesXgREwzhdhmjDmwgNZoTVFPuKHbMf8osrASwDrLWGvCW4w5gY3zkni9BX9JqTdtqs7w4kQpwx4kb8sGQ1015VmzoM"),
            Friend(id = 3, username = "Marcus", isOnline = true, avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAwgCCe0Uw5rd6HFCr5iMhXy2JGKzZkc2tynb7Wtfc8MdbksHfDSDCB_PTTtv0hBj5o17lKflfGsOeWW1YdoccJd5Mtkv84cpyFwpYWCsJWqBQjnCwjHFhFbzcsxZzRNdqam_Z5uH6H49V-de-aP54C8fR3W7Q_2n4Kg1eyUmg9szZZVE2FEKKQvawH-Klf3BXkoTQq-TN1NpgupR1oUbTVWlqKDspZbrkwsi_1CYBYOEFAE-2HcLDAZicbHvPBq4sRyb7BDlS_dvU"),
            Friend(id = 4, username = "Samira", isOnline = true, avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDauh3PIflslHSjCplf8TnCluMEf9RSpJCGsUuxcbFAmRqBp0efHL3revH5FipqYhrc2bk-vUbCRPi03T51CjzZyjjDyZVAwpc6DlcW6lFrSK1wbkmfqZYjKmgkn9Q0UUmvdGvGWe02J3eb6uOZ-nyjRuxFxXm01sMFZbvjzEyeOQAzFeqeTaTkqjm8bcMeP_odr1-Gz5wWB68dQAls05R7gEIjJWX1b8wazDiLgeb9ztJe7_h2fcxK9gG2i0CT_6hR7_waQZiABb0"),
            Friend(id = 5, username = "Leo", isOnline = false, avatarUrl = null)
        )

        _friendsList.value = sampleData
    }
}