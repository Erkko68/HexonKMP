package eric.bitria.hexon.game

import eric.bitria.hexon.game.data.def.BuildingDef
import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.def.PlacementType
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.def.PortDef
import eric.bitria.hexon.game.data.PortVertex
import eric.bitria.hexon.game.data.def.ResourceDef
import eric.bitria.hexon.game.data.ResourceId

class Board(
    private val availableResources: List<ResourceDef>,
    private val availableBuildings: List<BuildingDef>
) {

    // --- State Storage ---
    private val tiles = mutableMapOf<String, HexTile>()

    // Key: Normalized ID (Edge or Vertex string)
    // Value: The wrapper containing the Owner ID and the immutable Building Definition
    private val buildings = mutableMapOf<String, PlacedBuilding>()

    private val ports = mutableMapOf<PortVertex, PortDef>()

    var robberLocation: HexCoord = HexCoord(0, 0)
        private set

    // --- Initialization ---

    fun addTile(coord: HexCoord, resource: ResourceId?, number: Int) {
        if (resource != null) {
            require(availableResources.any { it.id == resource }) {
                "Resource '$resource' is not defined in the GameConfig."
            }
        }

        val tile = HexTile(coord, resource, number)
        tiles[HexCoord.getHexId(coord)] = tile

        // Auto-set robber if desert
        if (resource == null) {
            robberLocation = coord
        }
    }

    fun addPort(h1: HexCoord, h2: HexCoord, h3: HexCoord, resource: ResourceId?, ratio: Int) {
        if (resource != null) {
            require(availableResources.any { it.id == resource }) {
                "Resource '$resource' is not defined in the GameConfig."
            }
        }
        val id = HexCoord.getVertexId(h1, h2, h3)
        ports[id] = PortDef(h1, h2, h3, resource, ratio)
    }

    // --- Building Logic (Executor) ---

    /**
     * Places a Vertex building
     */
    fun placeVertexBuilding(
        typeId: BuildingId,
        ownerId: String,
        h1: HexCoord, h2: HexCoord, h3: HexCoord
    ): Boolean {
        // 1. Find Definition & Validate Type
        val def = availableBuildings.find { it.id == typeId }
        requireNotNull(def) { "Building Type '$typeId' is not defined in the GameConfig." }

        if (def.type != PlacementType.VERTEX) {
            throw IllegalArgumentException("Building '$typeId' is not a VERTEX building.")
        }

        val locId = HexCoord.getVertexId(h1, h2, h3)
        val existing = buildings[locId]

        // CASE 1: Upgrade Existing Building
        if (existing != null) {
            if (existing.ownerId != ownerId) return false

            // Check if this is a valid upgrade (e.g. Settlement -> City)
            if (existing.def.upgrade == typeId) {
                buildings[locId] = PlacedBuilding(ownerId, def)
                return true
            }
            return false
        }

        // CASE 2: Place New Building
        buildings[locId] = PlacedBuilding(ownerId, def)
        return true
    }

    /**
     * Places an Edge Building
     */
    fun placeEdgeBuilding(
        typeId: BuildingId,
        ownerId: String,
        h1: HexCoord, h2: HexCoord
    ): Boolean {
        // 1. Find Definition & Validate Type
        val def = availableBuildings.find { it.id == typeId }
        requireNotNull(def) { "Building Type '$typeId' is not defined in the GameConfig." }

        if (def.type != PlacementType.EDGE) {
            throw IllegalArgumentException("Building '$typeId' is not an EDGE building.")
        }

        val locId = HexCoord.getEdgeId(h1, h2)
        val existing = buildings[locId]

        // CASE 1: Upgrade Existing Building (Rare for edges, but supported)
        if (existing != null) {
            if (existing.ownerId != ownerId) return false

            if (existing.def.upgrade == typeId) {
                buildings[locId] = PlacedBuilding(ownerId, def)
                return true
            }
            return false
        }

        // CASE 2: Place New Building
        buildings[locId] = PlacedBuilding(ownerId, def)
        return true
    }

    fun getBuildingAt(locId: String): PlacedBuilding? = buildings[locId]

    // --- Validators ---

    fun canPlaceVertexBuilding(
        ownerId: String,
        h1: HexCoord, h2: HexCoord, h3: HexCoord,
        targetTypeId: BuildingId
    ): Boolean {
        require(availableBuildings.any { it.id == targetTypeId }) {
            "Building Type '$targetTypeId' is not defined in the GameConfig."
        }

        val locId = HexCoord.getVertexId(h1, h2, h3)
        val existing = buildings[locId]

        // 1. Check for Upgrade Validity
        if (existing != null) {
            if (existing.ownerId != ownerId) return false
            // Simplified: we have the def directly now
            return existing.def.upgrade == targetTypeId
        }

        // 2. Check Standard Placement (Distance Rule)
        return checkVertexPlacement(h1, h2, h3)
    }

    fun canPlaceEdgeBuilding(
        ownerId: String,
        h1: HexCoord, h2: HexCoord,
        targetTypeId: BuildingId
    ): Boolean {
        require(availableBuildings.any { it.id == targetTypeId }) {
            "Building Type '$targetTypeId' is not defined in the GameConfig."
        }

        val locId = HexCoord.getEdgeId(h1, h2)
        val existing = buildings[locId]

        // 1. Check for Upgrade Validity
        if (existing != null) {
            if (existing.ownerId != ownerId) return false
            return existing.def.upgrade == targetTypeId
        }

        // 2. Check Standard Placement
        return checkEdgePlacement(h1, h2, ownerId)
    }

    // --- Production Logic ---

    fun getProductionForRoll(roll: Int): Map<String, MutableMap<ResourceId, Int>> {
        val production = mutableMapOf<String, MutableMap<ResourceId, Int>>()

        val activeTiles = tiles.values.filter {
            it.numberToken == roll && it.coordinate != robberLocation
        }

        for (tile in activeTiles) {
            val resource = tile.resourceId ?: continue
            val vertices = getVerticesForHex(tile.coordinate)

            for (vertexId in vertices) {
                val building = buildings[vertexId] ?: continue

                // DYNAMIC PRODUCTION CALCULATION
                // We use the 'production' int from the definition directly
                val amount = building.def.production

                if (amount > 0) {
                    val playerProduction = production.getOrPut(building.ownerId) { mutableMapOf() }
                    playerProduction[resource] = (playerProduction[resource] ?: 0) + amount
                }
            }
        }
        return production
    }

    // --- Helpers (Robber & Geometry) ---

    fun moveRobber(target: HexCoord): List<PlayerId> {
        robberLocation = target
        return getVerticesForHex(target)
            .mapNotNull { buildings[it]?.ownerId }
            .distinct()
    }

    private fun getVerticesForHex(center: HexCoord): List<String> {
        val (q, r) = center
        val neighbors = listOf(
            HexCoord(q + 1, r - 1), HexCoord(q + 1, r), HexCoord(q, r + 1),
            HexCoord(q - 1, r + 1), HexCoord(q - 1, r), HexCoord(q, r - 1)
        )
        val vertexIds = mutableListOf<String>()
        for (i in 0 until 6) {
            val n1 = neighbors[i]
            val n2 = neighbors[(i + 1) % 6]
            vertexIds.add(HexCoord.getVertexId(center, n1, n2))
        }
        return vertexIds
    }

    // --- Private Validators ---

    private fun checkVertexPlacement(h1: HexCoord, h2: HexCoord, h3: HexCoord): Boolean {
        val neighbors = listOf(
            getNeighborVertex(h1, h2, h3),
            getNeighborVertex(h2, h3, h1),
            getNeighborVertex(h3, h1, h2)
        )
        return neighbors.none { buildings.containsKey(it) }
    }

    private fun checkEdgePlacement(h1: HexCoord, h2: HexCoord, ownerId: String): Boolean {
        val commonNeighbors = getCommonNeighbors(h1, h2)
        if (commonNeighbors.size != 2) return false

        val endpoint1 = HexCoord.getVertexId(h1, h2, commonNeighbors[0])
        val endpoint2 = HexCoord.getVertexId(h1, h2, commonNeighbors[1])

        return isConnectedAtVertex(endpoint1, ownerId, h1, h2, commonNeighbors[0]) ||
                isConnectedAtVertex(endpoint2, ownerId, h1, h2, commonNeighbors[1])
    }

    private fun getNeighborVertex(a: HexCoord, b: HexCoord, currentThird: HexCoord): String {
        val common = getCommonNeighbors(a, b)
        val other = common.firstOrNull { it != currentThird } ?: return ""
        return HexCoord.getVertexId(a, b, other)
    }

    private fun isConnectedAtVertex(
        vertexId: String, ownerId: String, h1: HexCoord, h2: HexCoord, h3: HexCoord
    ): Boolean {
        val building = buildings[vertexId]
        if (building != null && building.ownerId == ownerId) return true

        val edgeA = HexCoord.getEdgeId(h1, h3)
        val edgeB = HexCoord.getEdgeId(h2, h3)
        return (buildings[edgeA]?.ownerId == ownerId) || (buildings[edgeB]?.ownerId == ownerId)
    }

    private fun getCommonNeighbors(h1: HexCoord, h2: HexCoord): List<HexCoord> {
        val n1 = getNeighbors(h1)
        val n2 = getNeighbors(h2)
        return n1.intersect(n2.toSet()).toList()
    }

    private fun getNeighbors(center: HexCoord): List<HexCoord> {
        val (q, r) = center
        return listOf(
            HexCoord(q + 1, r - 1), HexCoord(q + 1, r), HexCoord(q, r + 1),
            HexCoord(q - 1, r + 1), HexCoord(q - 1, r), HexCoord(q, r - 1)
        )
    }
}

// --- Data Classes ---

/**
 * A wrapper class used internally by Board to act as an "Instance" of a building.
 * It combines the static definition with the dynamic owner.
 */
data class PlacedBuilding(
    val ownerId: PlayerId,
    val def: BuildingDef
)

data class HexTile(
    val coordinate: HexCoord,
    val resourceId: String?, // Null = Desert
    val numberToken: Int    // 7 = Desert
)