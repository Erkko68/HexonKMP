package eric.bitria.hexon.data.serializable.actions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("TRADE")
data class TradeAction(
    override val type: String = "TRADE",
    val fromPlayerId: String,
    val toPlayerId: String,
    val offer: ResourceBundle,
    val request: ResourceBundle
) : HexonAction()

@Serializable
data class ResourceBundle(
    val wood: Int = 0,
    val brick: Int = 0,
    val sheep: Int = 0,
    val wheat: Int = 0,
    val ore: Int = 0
)
