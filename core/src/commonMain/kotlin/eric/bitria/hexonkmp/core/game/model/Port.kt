package eric.bitria.hexonkmp.core.game.model

import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import kotlinx.serialization.Serializable

// A harbor: a single vertex that, when a player owns a building on it, lowers
// their bank-trade ratio. [resource] null means a generic port (the lower ratio
// applies to every resource); otherwise it's a specific port (the ratio applies
// only when giving that resource). [ratio] is how many of the give-resource the
// bank takes for one of any other (classic: 3 for generic, 2 for specific).
//
// One entry per vertex: a physical harbor touches two coastline vertices, so it
// appears as two Port rows sharing the same [resource] and [ratio].
@Serializable
data class Port(
    val vertex: Vertex,
    val resource: Resource?,
    val ratio: Int,
)