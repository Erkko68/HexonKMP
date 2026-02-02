package eric.bitria.hexon.ws

import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.ResourceId
import kotlinx.serialization.Serializable

@Serializable
sealed class GameplayIntent : GameplayMessage() {

    // --- Turn Management ---

    @Serializable
    data object EndTurn: GameplayIntent()

    // --- Dynamic Construction ---
    @Serializable
    data class Build(
        val buildingId: BuildingId,

        // Coordinates:
        // For EDGE: hexA, hexB required. hexC must be null.
        // For VERTEX: hexA, hexB, hexC required.
        val h1: HexCoord,
        val h2: HexCoord,
        val h3: HexCoord? = null,
    ) : GameplayIntent()

    // --- Trade ---
    @Serializable
    data class ProposeTrade(
        val give: Map<ResourceId, Int>,
        val want: Map<ResourceId, Int>,
    ) : GameplayIntent()

    @Serializable
    data class RespondToTrade(
        val offererId: PlayerId,
        val accepted: Boolean,
    ) : GameplayIntent()

    @Serializable
    data class ConfirmTrade(
        val responderId: PlayerId,
    ) : GameplayIntent()

    @Serializable
    data class CancelTrade(
        val offererId: PlayerId
    ) : GameplayIntent()

    @Serializable
    data class ExchangeWithBank(
        val give: Map<ResourceId, Int>,
        val get: Map<ResourceId, Int>,
    ) : GameplayIntent()

    // --- Robber ---
    @Serializable
    data class MoveRobber(
        val hexA: HexCoord,   // Target Hex
    ) : GameplayIntent()
}