package eric.bitria.hexon.game

import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.PortVertex
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.config.GameConfig
import eric.bitria.hexon.game.data.def.BuildingDef
import eric.bitria.hexon.game.data.def.PlacementType
import eric.bitria.hexon.game.data.def.PortDef
import eric.bitria.hexon.game.data.def.ResourceDef
import kotlin.random.Random

class Board {

    // --- Definitions ---
    val availableResources = mutableMapOf<ResourceId,ResourceDef>()
    val availableBuildings = mutableMapOf<ResourceId,BuildingDef>()

    // --- State Storage ---
    val tiles = mutableMapOf<String, HexTile>()
    val buildings = mutableMapOf<String, PlacedBuilding>() // Key: Vertex/Edge ID
    val ports = mutableMapOf<PortVertex, PortDef>()

    var robberLocation: HexCoord = HexCoord(0, 0)
        private set

    // --- Initialization ---

    fun initialize(config: GameConfig) {
        tiles.clear()
        ports.clear()
        buildings.clear()

        // 0. Initialize Definitions
        config.resourceDefs.forEach {
            resourceDef -> availableResources[resourceDef.id] = resourceDef
        }

        config.buildingDefs.forEach {
            buildingDef -> availableBuildings[buildingDef.id] = buildingDef
        }

        // 1. Create a deterministic Random instance using the seed hash
        val rng = Random(config.seed.hashCode())

        // 2. Prepare "Deck" of tokens using the seeded RNG
        val resourcePool = config.tileResourcePool.toMutableList().apply {
            shuffle(rng) // Shuffles identically on all clients
        }

        val numberPool = config.tileNumberPool.toMutableList().apply {
            shuffle(rng)
        }

        // 3. Place Tiles
        for (coord in config.gridCoords) {
            val fixed = config.fixedTiles[coord]

            if (fixed != null) {
                // Fixed Tile
                addTile(coord, fixed.resource, fixed.number)
            } else {
                // Random Tile
                if (resourcePool.isNotEmpty() && numberPool.isNotEmpty()) {
                    val resource = resourcePool.removeAt(0)
                    val number = numberPool.removeAt(0)
                    addTile(coord, resource, number)
                } else {
                    throw IllegalStateException("Config has more coordinates than resources available.")
                }
            }
        }

        // 4. Place Ports
        config.ports.forEach {
            addPort(it.h1, it.h2, it.h3, it.resourceId, it.ratio)
        }
    }

    fun addTile(coord: HexCoord, resource: ResourceId, number: Int) {
        tiles[HexCoord.getHexId(coord)] = HexTile(coord, resource, number)
        if (resource == "desert") robberLocation = coord
    }

    fun addPort(h1: HexCoord, h2: HexCoord, h3: HexCoord, resource: ResourceId?, ratio: Int) {
        val id = HexCoord.getVertexId(h1, h2, h3)
        ports[id] = PortDef(h1, h2, h3, resource, ratio)
    }

    // --- Building Placement ---

    fun placeVertexBuilding(
        typeId: BuildingId, ownerId: String,
        h1: HexCoord, h2: HexCoord, h3: HexCoord
    ): Boolean {
        if (!canPlaceVertexBuilding(ownerId, h1, h2, h3, typeId)) return false

        val def = availableBuildings[typeId] ?: throw IllegalArgumentException("Unknown building type: $typeId")

        buildings[HexCoord.getVertexId(h1, h2, h3)] = PlacedBuilding(ownerId, def)
        return true
    }

    fun placeEdgeBuilding(
        typeId: BuildingId, ownerId: String,
        h1: HexCoord, h2: HexCoord
    ): Boolean {
        if (!canPlaceEdgeBuilding(ownerId, h1, h2, typeId)) return false

        val def = availableBuildings[typeId] ?: throw IllegalArgumentException("Unknown building type: $typeId")
        buildings[HexCoord.getEdgeId(h1, h2)] = PlacedBuilding(ownerId, def)
        return true
    }

    fun getBuildingAt(locId: String): PlacedBuilding? = buildings[locId]

    // --- Validation Rules ---

    fun canPlaceVertexBuilding(
        ownerId: String, h1: HexCoord, h2: HexCoord, h3: HexCoord, targetTypeId: BuildingId
    ): Boolean {
        if (h1 == h2 || h1 == h3 || h2 == h3) return false
        if (!hasTileConnection(h1, h2, h3)) return false

        val def = availableBuildings[targetTypeId] ?: throw IllegalArgumentException("Unknown building type: $targetTypeId")
        if (def.type != PlacementType.VERTEX) return false

        val locId = HexCoord.getVertexId(h1, h2, h3)
        val existing = buildings[locId]

        return if (existing != null) {
            existing.ownerId == ownerId && existing.def.upgrade == targetTypeId
        } else {
            checkVertexDistanceRule(h1, h2, h3)
        }
    }

    fun canPlaceEdgeBuilding(
        ownerId: String, h1: HexCoord, h2: HexCoord, targetTypeId: BuildingId
    ): Boolean {
        if (h1 == h2) return false
        if (!hasTileConnection(h1, h2)) return false

        val def = availableBuildings[targetTypeId] ?: throw IllegalArgumentException("Unknown building type: $targetTypeId")
        if (def.type != PlacementType.EDGE) throw IllegalArgumentException("Unknown building type: $targetTypeId")

        val locId = HexCoord.getEdgeId(h1, h2)
        val existing = buildings[locId]

        return if (existing != null) {
            existing.ownerId == ownerId && existing.def.upgrade == targetTypeId
        } else {
            checkEdgeConnectionRule(h1, h2, ownerId)
        }
    }

    // --- Discovery (AI/UI Helpers) ---

    fun getAvailableVertexPlacements(
        ownerId: String, buildingId: BuildingId
    ): List<Triple<HexCoord, HexCoord, HexCoord>> {
        val validSpots = mutableListOf<Triple<HexCoord, HexCoord, HexCoord>>()
        val visitedIds = mutableSetOf<String>()

        for (tile in tiles.values) {
            for ((h1, h2, h3) in getCornerCoords(tile.coordinate)) {
                val id = HexCoord.getVertexId(h1, h2, h3)
                if (visitedIds.add(id)) { // Dedup using ID
                    if (canPlaceVertexBuilding(ownerId, h1, h2, h3, buildingId)) {
                        validSpots.add(Triple(h1, h2, h3))
                    }
                }
            }
        }
        return validSpots
    }

    fun getAvailableEdgePlacements(
        ownerId: String, buildingId: BuildingId
    ): List<Pair<HexCoord, HexCoord>> {
        val validSpots = mutableListOf<Pair<HexCoord, HexCoord>>()
        val visitedIds = mutableSetOf<String>()

        for (tile in tiles.values) {
            for ((h1, h2) in getEdgeCoords(tile.coordinate)) {
                val id = HexCoord.getEdgeId(h1, h2)
                if (visitedIds.add(id)) { // Dedup using ID
                    if (canPlaceEdgeBuilding(ownerId, h1, h2, buildingId)) {
                        validSpots.add(Pair(h1, h2))
                    }
                }
            }
        }
        return validSpots
    }

    // --- Production & Gameplay ---

    fun getProductionForRoll(roll: Int): Map<String, MutableMap<ResourceId, Int>> {
        val production = mutableMapOf<String, MutableMap<ResourceId, Int>>()

        val activeTiles = tiles.values.filter {
            it.numberToken == roll && it.coordinate != robberLocation
        }

        for (tile in activeTiles) {
            val resource = tile.resourceId ?: continue
            val vertexIds = getCornerCoords(tile.coordinate).map { (h1, h2, h3) ->
                HexCoord.getVertexId(h1, h2, h3)
            }

            for (vertexId in vertexIds) {
                val building = buildings[vertexId] ?: continue
                val amount = building.def.production

                if (amount > 0) {
                    val playerProd = production.getOrPut(building.ownerId) { mutableMapOf() }
                    playerProd[resource] = (playerProd[resource] ?: 0) + amount
                }
            }
        }
        return production
    }

    fun moveRobber(target: HexCoord): List<PlayerId> {
        robberLocation = target
        return getCornerCoords(target)
            .map { HexCoord.getVertexId(it.first, it.second, it.third) }
            .mapNotNull { buildings[it]?.ownerId }
            .distinct()
    }

    // --- Private Validators ---

    private fun checkVertexDistanceRule(h1: HexCoord, h2: HexCoord, h3: HexCoord): Boolean {
        // A vertex is valid if NONE of its 3 neighbors are occupied
        val neighbors = listOf(
            getNeighborVertexId(h1, h2, h3),
            getNeighborVertexId(h2, h3, h1),
            getNeighborVertexId(h3, h1, h2)
        )
        return neighbors.none { buildings.containsKey(it) }
    }

    private fun checkEdgeConnectionRule(h1: HexCoord, h2: HexCoord, ownerId: String): Boolean {
        // An edge is valid if it connects to an existing road or building owned by player
        val commonNeighbors = getCommonNeighbors(h1, h2)
        if (commonNeighbors.size != 2) return false

        return commonNeighbors.any { neighbor ->
            val vertexId = HexCoord.getVertexId(h1, h2, neighbor)
            isConnectedAtVertex(vertexId, ownerId, h1, h2, neighbor)
        }
    }

    private fun isConnectedAtVertex(
        vertexId: String, ownerId: String, h1: HexCoord, h2: HexCoord, h3: HexCoord
    ): Boolean {
        // 1. Direct connection: Player owns the building at this vertex
        val building = buildings[vertexId]
        if (building != null && building.ownerId == ownerId) return true

        // 2. Road connection: Player owns one of the other 2 edges coming into this vertex
        // The vertex is (h1, h2, h3). The edge we are building is (h1, h2).
        // The other edges are (h1, h3) and (h2, h3).
        val edgeA = HexCoord.getEdgeId(h1, h3)
        val edgeB = HexCoord.getEdgeId(h2, h3)
        return (buildings[edgeA]?.ownerId == ownerId) || (buildings[edgeB]?.ownerId == ownerId)
    }

    private fun hasTileConnection(h1: HexCoord, h2: HexCoord, h3: HexCoord? = null): Boolean {
        val t1 = tiles.containsKey(HexCoord.getHexId(h1))
        val t2 = tiles.containsKey(HexCoord.getHexId(h2))
        val t3 = h3?.let { tiles.containsKey(HexCoord.getHexId(it)) } ?: false
        return t1 || t2 || t3
    }

    // --- Geometry Helpers ---

    private fun getNeighbors(center: HexCoord): List<HexCoord> {
        val (q, r) = center
        return listOf(
            HexCoord(q + 1, r - 1), HexCoord(q + 1, r), HexCoord(q, r + 1),
            HexCoord(q - 1, r + 1), HexCoord(q - 1, r), HexCoord(q, r - 1)
        )
    }

    private fun getCornerCoords(center: HexCoord): List<Triple<HexCoord, HexCoord, HexCoord>> {
        val n = getNeighbors(center)
        // A corner is defined by the Center + Neighbor[i] + Neighbor[i+1]
        return (0 until 6).map { i ->
            Triple(center, n[i], n[(i + 1) % 6])
        }
    }

    private fun getEdgeCoords(center: HexCoord): List<Pair<HexCoord, HexCoord>> {
        // An edge is defined by the Center + Neighbor[i]
        return getNeighbors(center).map { neighbor -> Pair(center, neighbor) }
    }

    private fun getCommonNeighbors(h1: HexCoord, h2: HexCoord): List<HexCoord> {
        val n1 = getNeighbors(h1)
        val n2 = getNeighbors(h2)
        return n1.intersect(n2.toSet()).toList()
    }

    private fun getNeighborVertexId(pivot1: HexCoord, pivot2: HexCoord, tail: HexCoord): String {
        // Finds the "other" vertex connected to (pivot1, pivot2) that isn't 'tail'
        val common = getCommonNeighbors(pivot1, pivot2)
        val extension = common.firstOrNull { it != tail } ?: return ""
        return HexCoord.getVertexId(pivot1, pivot2, extension)
    }
}

// --- Data Classes ---

data class PlacedBuilding(
    val ownerId: PlayerId,
    val def: BuildingDef
)

data class HexTile(
    val coordinate: HexCoord,
    val resourceId: String?, // Null = Desert
    val numberToken: Int     // 0 = Desert
)