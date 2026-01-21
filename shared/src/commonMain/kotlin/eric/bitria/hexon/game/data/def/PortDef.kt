package eric.bitria.hexon.game.data.def

import eric.bitria.hexon.game.data.HexCoord
import kotlinx.serialization.Serializable

@Serializable
data class PortDef(
    val h1: HexCoord,
    val h2: HexCoord,
    val h3: HexCoord,
    val resourceId: String?,
    val ratio: Int
)