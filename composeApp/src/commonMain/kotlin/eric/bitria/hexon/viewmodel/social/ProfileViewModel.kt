package eric.bitria.hexon.viewmodel.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eric.bitria.hexon.ui.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val sampleHistory = listOf(
        GameHistoryItem(1, true, "vs. Zoe, Marcus", "2 days ago", 15),
        GameHistoryItem(2, false, "vs. Samira", "3 days ago", -10),
        GameHistoryItem(3, true, "vs. Leo", "5 days ago", 12),
        GameHistoryItem(4, true, "vs. Marcus, Zoe", "1 week ago", 18)
    )

    private val _uiState = MutableStateFlow(ProfileUiState(gameHistory = sampleHistory))
    val uiState = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        observeProfile()
        fetchProfile(forceRefresh = false)
    }

    private fun observeProfile() {
        viewModelScope.launch {
            userRepository.profile.collect { profile ->
                profile?.let { response ->
                    _uiState.update { state ->
                        state.copy(
                            username = response.username,
                            stats = UserStats(
                                wins = response.stats.wins.toString(),
                                streak = "-",
                                winRate = "${calculateWinRate(response.stats.wins, response.stats.losses).toInt()}%"
                            ),
                            error = null
                        )
                    }
                }
            }
        }
    }

    private fun fetchProfile(forceRefresh: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.getProfile(forceRefresh = forceRefresh)
            } catch (e: Exception) {
                // Only show error if we don't have any cached data to display
                if (_uiState.value.username.isEmpty()) {
                    _uiState.update { it.copy(error = e.message) }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(error = null) }
        fetchProfile(forceRefresh = true)
    }

    private fun calculateWinRate(won: Int, lost: Int): Double {
        val total = won + lost
        val rate = if (total > 0) (won.toDouble() / total) * 100 else 0.0
        return rate
    }
}