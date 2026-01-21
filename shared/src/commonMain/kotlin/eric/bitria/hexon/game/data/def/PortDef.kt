package eric.bitria.hexon.game.data.def

import eric.bitria.hexon.game.data.HexCoord
import kotlinx.serialization.Serializable

@Serializable
data class PortDef(
    val hexA: HexCoord,
    val hexB: HexCoord,
    val hexC: HexCoord,
    val resourceId: String?,
    val ratio: Int
)