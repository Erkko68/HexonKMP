package eric.bitria.hexonkmp.core.game.model.board

import kotlinx.serialization.Serializable

// The five Catan resources. Kept as an enum so build costs, hands, and trades
// are data, not hardcoded fields.
@Serializable
enum class Resource { BRICK, LUMBER, WOOL, GRAIN, ORE }

// Terrain types of a tile. Each yields one resource (or none, for the desert).
// The terrain → resource mapping is part of the domain, not the engine.
@Serializable
enum class Terrain(val resource: Resource?) {
    HILLS(Resource.BRICK),
    FOREST(Resource.LUMBER),
    PASTURE(Resource.WOOL),
    FIELDS(Resource.GRAIN),
    MOUNTAINS(Resource.ORE),
    DESERT(null),
}
