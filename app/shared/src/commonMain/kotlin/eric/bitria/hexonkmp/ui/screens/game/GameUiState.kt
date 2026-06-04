package eric.bitria.hexonkmp.ui.screens.game

import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.board.Axial
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Vertex

// What the player is currently placing (drives the board's ghost markers).
enum class BuildMode { NONE, SETTLEMENT, ROAD, CITY }

// Everything the HUD needs to render the action bar and dev-card row, computed
// once per state change in the ViewModel. The Screen never reads GamePhase or
// game-state fields directly to decide what to show — it just reads these flags.
data class BuildOptions(
    // Build placement availability
    val canSettlement: Boolean = false,
    val canRoad: Boolean = false,
    val canCity: Boolean = false,
    val canBuyDevCard: Boolean = false,
    // Ghost markers shown on the board while a build mode is armed
    val ghostSettlements: List<Vertex> = emptyList(),
    val ghostRoads: List<Edge> = emptyList(),
    val ghostCities: List<Vertex> = emptyList(),
    // Tiles the current player may move the robber to (Robber phase only)
    val robberTargets: List<Axial> = emptyList(),
    // Trade tab: enabled for the current player + opponents with incoming offers
    val canTrade: Boolean = false,
    // Notification dot on the Trade button
    val tradeBadge: Boolean = false,
    // End Turn: shown only in Play phase, enabled only on the local player's turn
    val showEndTurn: Boolean = false,
    val canEndTurn: Boolean = false,
    // Which dev card types the local player may play right now
    val playableDevCards: Set<DevCard> = emptySet(),
) {
    companion object {
        val NONE = BuildOptions()
    }
}

sealed class GameUiState {
    data object Idle : GameUiState()
    data object Connecting : GameUiState()
    data class Waiting(val gameId: String, val connected: Int = 1, val needed: Int = 2) : GameUiState()
    data class InGame(
        val gameId: String,
        val state: GameState,
        val myPlayerId: PlayerId,
        val buildMode: BuildMode = BuildMode.NONE,
        val notice: String? = null,
    ) : GameUiState() {
        val isMyTurn: Boolean get() = state.currentPlayer == myPlayerId
    }
    data class Error(val message: String) : GameUiState()
}
