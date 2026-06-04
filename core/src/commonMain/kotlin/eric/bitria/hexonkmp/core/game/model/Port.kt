package eric.bitria.hexonkmp.core.game.model

import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Resource
import kotlinx.serialization.Serializable

// A harbor placed on a coastline [edge]: a player with a building on EITHER of the
// edge's two vertices gets the discount. [resource] null = generic port (lowers
// every resource's ratio); otherwise specific (lowers only that resource). [ratio]
// is how many of the give-resource the bank takes for one of any other (classic:
// 3 for generic, 2 for specific).
@Serializable
data class Port(
    val edge: Edge,
    val resource: Resource?,
    val ratio: Int,
)

// A harbor "recipe" without a position — the multiset a scenario wants placed (like
// terrainBag / numberTokens). BoardGenerator shuffles these onto coastline edges.
@Serializable
data class PortKind(
    val resource: Resource?,
    val ratio: Int,
)
