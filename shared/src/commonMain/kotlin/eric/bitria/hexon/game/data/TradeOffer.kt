package eric.bitria.hexon.game.data

import kotlinx.serialization.Serializable

@Serializable
data class TradeOffer(
    val give: Map<ResourceId, Int>,
    val want: Map<ResourceId, Int>
)