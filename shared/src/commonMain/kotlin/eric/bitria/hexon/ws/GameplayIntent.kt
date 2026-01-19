package eric.bitria.hexon.ws

import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.TradeOffer
import kotlinx.serialization.Serializable

@Serializable
sealed class GameplayIntent : GameplayMessage() {

    // --- Turn Management ---

    @Serializable
    data class EndTurn(
        override var senderId: PlayerId? = null
    ) : GameplayIntent()

    // --- Dynamic Construction ---
    @Serializable
    data class Build(
        val buildingId: BuildingId,

        // Coordinates:
        // For EDGE: hexA, hexB required. hexC must be null.
        // For VERTEX: hexA, hexB, hexC required.
        val hexA: HexCoord,
        val hexB: HexCoord,
        val hexC: HexCoord? = null,

        override var senderId: String? = null
    ) : GameplayIntent()

    // --- Trade ---
    @Serializable
    data class ProposeTrade(
        val offer: TradeOffer,
        override var senderId: String? = null
    ) : GameplayIntent()

    @Serializable
    data class RespondToTrade(
        val offererId: PlayerId,
        val accepted: Boolean,
        override var senderId: String? = null
    ) : GameplayIntent()

    @Serializable
    data class ConfirmTrade(
        val responderId: PlayerId,
        val accepted: Boolean,
        override var senderId: String? = null
    ) : GameplayIntent()

    @Serializable
    data class ExchangeWithBank(
        val give: Map<ResourceId, Int>,
        val get: Map<ResourceId, Int>,
        override var senderId: String? = null
    ) : GameplayIntent()

    // --- Robber ---
    @Serializable
    data class MoveRobber(
        val hexA: HexCoord,   // Target Hex
        override var senderId: String? = null
    ) : GameplayIntent()
}