package eric.bitria.hexon.ws

import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.TradeOffer
import kotlinx.serialization.Serializable

@Serializable
sealed class GameplayIntent : GameplayMessage() {

    // --- Turn Management ---

    @Serializable
    data class EndTurn(
        override var senderId: String? = null
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
        val receiverPlayerId: String? = null, // null = Public
        override var senderId: String? = null
    ) : GameplayIntent()

    @Serializable
    data class RespondToTrade(
        val tradeId: String,
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
        val hexLocation: HexCoord,   // Target Hex
        val victimId: String?,       // Optional (might trigger simple move)
        override var senderId: String? = null
    ) : GameplayIntent()
}