package eric.bitria.hexonkmp.ui.screens.lobby

// The lobby/main-menu states: choosing a name, finding a game, and waiting for the
// room to fill. The in-game state lives in its own screen (navigated to on start).
sealed interface LobbyUiState {
    data object Idle : LobbyUiState
    data object Connecting : LobbyUiState
    // [countdownSeconds] is the auto-start delay remaining once the minimum is met
    // (null before then); the screen ticks it down locally.
    data class Waiting(
        val connected: Int,
        val needed: Int,
        val countdownSeconds: Int? = null,
    ) : LobbyUiState
    data class Error(val message: String) : LobbyUiState
}
