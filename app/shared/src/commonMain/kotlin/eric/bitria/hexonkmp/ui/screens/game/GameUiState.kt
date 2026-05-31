package eric.bitria.hexonkmp.ui.screens.game

sealed class GameUiState {
    data object Idle : GameUiState()
    data object Connecting : GameUiState()
    data class Waiting(val gameId: String, val connected: Int = 1, val needed: Int = 2) : GameUiState()
    data class InGame(val gameId: String) : GameUiState()
    data class Error(val message: String) : GameUiState()
}
