package eric.bitria.hexon.ws

import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.BuildingSnapshot
import eric.bitria.hexon.game.data.GameConfig
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.enums.GameErrorCode
import eric.bitria.hexon.game.data.PlayerSnapshot
import eric.bitria.hexon.game.data.TradeOffer
import eric.bitria.hexon.game.data.enums.UpdateReason
import kotlinx.serialization.Serializable

@Serializable
sealed class GameplayEvent : GameplayMessage() {

    // --- Initialization ---
    @Serializable
    data class GameConfigLoaded(
        val config: GameConfig,
        override var senderId: String? = "Server"
    ) : GameplayEvent()

    @Serializable
    data class GameSnapshot(
        val boardState: List<BuildingSnapshot>, // All buildings currently on board
        val players: List<PlayerSnapshot>,
        val currentTurnPlayerId: String,
        val diceResult: List<Int>? = null,
        val robberLocation: HexCoord? = null,
        override var senderId: String? = "Server"
    ) : GameplayEvent()

    // --- State Changes ---
    @Serializable
    data class RobberUpdated(
        val location: HexCoord,
        override var senderId: String? = "Server"
    ): GameplayEvent()

    @Serializable
    data class ResourcesUpdated(
        val playerId: PlayerId,
        val changes: Map<ResourceId, Int>, // e.g. {"wood": -1}
        val reason: UpdateReason,
        override var senderId: String? = "Server"
    ) : GameplayEvent()

    @Serializable
    data class ObjectBuilt(
        val playerId: PlayerId,
        val buildingId: BuildingId,

        // Exact location where it appeared
        val hexA: HexCoord,
        val hexB: HexCoord,
        val hexC: HexCoord? = null,

        override var senderId: String? = "Server"
    ) : GameplayEvent()

    @Serializable
    data class DiceRolled(
        val values: Pair<Int,Int>,
        val sum: Int,
        val sourcePlayerId: PlayerId,
        override var senderId: String? = "Server"
    ) : GameplayEvent()

    @Serializable
    data class TurnChanged(
        val newPlayerId: PlayerId,
        override var senderId: String? = "Server"
    ) : GameplayEvent()

    // --- Trade Interactions ---
    @Serializable
    data class TradeProposed(
        val tradeId: String,
        val proposerId: String,
        val offer: TradeOffer,
        override var senderId: String? = "Server"
    ) : GameplayEvent()

    @Serializable
    data class TradeCompleted(
        val tradeId: String,
        val participantIds: List<String>,
        override var senderId: String? = "Server"
    ) : GameplayEvent()

    // --- System ---
    @Serializable
    data class GameError(
        val message: String,
        val code: GameErrorCode,
        override var senderId: String? = "Server"
    ) : GameplayEvent()

    @Serializable
    data class GameOver(
        val winnerId: String,
        val stats: Map<String, String>,
        override var senderId: String? = "Server"
    ) : GameplayEvent()
}