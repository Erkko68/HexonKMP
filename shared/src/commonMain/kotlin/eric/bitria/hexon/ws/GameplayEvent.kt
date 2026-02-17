package eric.bitria.hexon.ws

import eric.bitria.hexon.game.GamePlayer
import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.BuildingSnapshot
import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.PlayerSnapshot
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.config.GameConfig
import eric.bitria.hexon.game.data.enums.GameErrorCode
import eric.bitria.hexon.game.data.enums.TurnPhase
import eric.bitria.hexon.game.data.enums.UpdateReason
import kotlinx.serialization.Serializable

@Serializable
sealed class GameplayEvent : GameplayMessage() {

    // --- Initialization ---
    @Serializable
    data class GameConfigLoaded(
        val config: GameConfig,
    ) : GameplayEvent()

    @Serializable
    data class PlayerJoined(
        val player: PlayerSnapshot,
    ) : GameplayEvent()

    @Serializable
    data class GamePlayerStats(
        val player: GamePlayer,
    ) : GameplayEvent()

    @Serializable
    data class GameSnapshot(
        val boardState: List<BuildingSnapshot>, // All buildings currently on board
        val players: List<PlayerSnapshot>,
        val currentTurnPlayerId: String,
        val diceResult: List<Int>? = null,
        val robberLocation: HexCoord? = null,
    ) : GameplayEvent()

    // --- State Changes ---
    @Serializable
    data class RobberUpdated(
        val location: HexCoord,
    ): GameplayEvent()

    @Serializable
    data class ResourcesUpdated(
        val changes: Map<ResourceId, Int>, // e.g. {"wood": -1}
        val reason: UpdateReason,
    ) : GameplayEvent()

    @Serializable
    data class ResourceCountUpdated(
        val playerId: PlayerId,
        val changes: Int,
        val reason: UpdateReason,
    ) : GameplayEvent()

    @Serializable
    data class ObjectBuilt(
        val playerId: PlayerId,
        val buildingId: BuildingId,

        // Exact location where it appeared
        val hexA: HexCoord,
        val hexB: HexCoord,
        val hexC: HexCoord? = null,
    ) : GameplayEvent()

    @Serializable
    data class DiceRolled(
        val values: Pair<Int,Int>,
    ) : GameplayEvent()

    @Serializable
    data class TurnChanged(
        val turnPhase: TurnPhase,
        val newPlayerId: PlayerId,
    ) : GameplayEvent()

    // --- Trade Interactions ---
    @Serializable
    data class TradeProposed(
        val give: Map<ResourceId, Int>,
        val want: Map<ResourceId, Int>,
        val offererId: PlayerId
    ) : GameplayEvent()

    @Serializable
    data class TradeResponse(
        val offererId: PlayerId,
        val responderId: PlayerId,
        val accepted: Boolean,
    ) : GameplayEvent()

    @Serializable
    data class TradeCompleted(
        val responderId: PlayerId,
        val offererId: PlayerId,
    ) : GameplayEvent()

    @Serializable
    data class TradeCancelled(
        val offererId: PlayerId,
    ) : GameplayEvent()

    // --- System ---
    @Serializable
    data class GameError(
        val message: String,
        val code: GameErrorCode,
    ) : GameplayEvent()

    @Serializable
    data class GameOver(
        val winnerId: String
    ) : GameplayEvent()
}