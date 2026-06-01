package eric.bitria.hexonkmp.core.game.model

import eric.bitria.hexonkmp.core.game.model.board.Edge
import kotlinx.serialization.Serializable

// A road placed on a board edge, owned by a player. Roads form the network that
// settlements (after setup) and further roads must connect to.
@Serializable
data class Road(
    val owner: PlayerId,
    val edge: Edge,
)
