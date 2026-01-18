package eric.bitria.hexon.game.data

import kotlinx.serialization.Serializable

@Serializable
data class BuildingSnapshot(
    val ownerId: PlayerId,
    val typeId: BuildingId,

    // Location
    val hexA: HexCoord,
    val hexB: HexCoord,
    val hexC: HexCoord? = null
)

@Serializable
data class PlayerSnapshot(
    val id: PlayerId,
    val name: String,
    val color: String,
    val victoryPoints: Int,

    // Public Info Only
    val resourceCount: Int,
    val devCardCount: Int,
    val playedKnights: Int,
    val longestRoad: Boolean,
    val largestArmy: Boolean
)