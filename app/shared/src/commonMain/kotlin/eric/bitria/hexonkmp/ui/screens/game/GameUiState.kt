package eric.bitria.hexonkmp.ui.screens.game

import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.PlayerId

sealed class GameUiState {
    data object Idle : GameUiState()
    data object Connecting : GameUiState()
    data class Waiting(val gameId: String, val connected: Int = 1, val needed: Int = 2) : GameUiState()
    data class InGame(
        val gameId: String,
        val state: GameState,
        val myPlayerId: PlayerId,
        val notice: String? = null,
    ) : GameUiState() {
        val isMyTurn: Boolean get() = state.currentPlayer == myPlayerId
    }
    data class Error(val message: String) : GameUiState()
}
