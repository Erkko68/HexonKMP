package eric.bitria.hexon.game

import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.ResourceId


class Board {

    // --- State Storage ---
    // Key: "q,r"
    private val tiles = mutableMapOf<String, HexTile>()

    // Key: Normalized ID (Edge or Vertex string)
    // Value: The building placed there
    private val buildings = mutableMapOf<String, Building>()

    // Key: Normalized Edge ID
    private val ports = mutableMapOf<String, Port>()

    // Robber State
    var robberLocation: HexCoord = HexCoord(0, 0)
        private set

    // --- Initialization ---

    fun addTile(coord: HexCoord, resource: String?, number: Int) {
        val tile = HexTile(coord, resource, number)
        tiles[HexCoord.getHexId(coord)] = tile

        // Auto-set robber if desert
        if (resource == null) {
            robberLocation = coord
        }
    }

    fun addPort(h1: HexCoord, h2: HexCoord, resource: String?, ratio: Int) {
        val id = HexCoord.getEdgeId(h1, h2)
        ports[id] = Port(id, resource, ratio)
    }

    // --- Building Logic ---

    /**
     * @return true if the spot was empty and successfully built
     */
    fun placeBuilding(typeId: String, ownerId: String, locId: String, isVertex: Boolean): Boolean {
        if (buildings.containsKey(locId)) return false

        buildings[locId] = Building(
            id = typeId,
            ownerId = ownerId,
            type = if (isVertex) "VERTEX" else "EDGE"
        )
        return true
    }

    fun getBuildingAt(locId: String): Building? = buildings[locId]

    // --- Production Logic (The Core) ---

    /**
     * Given a dice roll, calculate who gets what.
     * @return Map of PlayerID -> List of ResourceIDs
     */
    fun getProductionForRoll(roll: Int): Map<String, List<ResourceId>> {
        val production = mutableMapOf<String, MutableList<ResourceId>>()

        // 1. Find all tiles matching the dice roll
        val activeTiles = tiles.values.filter {
            it.numberToken == roll && it.coordinate != robberLocation
        }

        // 2. For each active tile, check its 6 corners (vertices)
        for (tile in activeTiles) {
            val resource = tile.resourceId ?: continue

            // Get the 6 vertex IDs around this hex
            val vertices = getVerticesForHex(tile.coordinate)

            for (vertexId in vertices) {
                val building = buildings[vertexId] ?: continue

                // Logic: Settlement = 1 res, City = 2 res
                // In dynamic system: we might look up "productionMultiplier" in config
                // For now, hardcode standard logic or inject it:
                val amount = if (building.id == "city") 2 else 1

                val playerList = production.getOrPut(building.ownerId) { mutableListOf() }
                repeat(amount) { playerList.add(resource) }
            }
        }
        return production
    }

    // --- Helpers ---

    fun moveRobber(target: HexCoord) {
        robberLocation = target
    }

    /**
     * Calculates the 6 canonical Vertex IDs surrounding a generic Hex (q,r).
     * This relies on understanding Hex Geometry.
     * * Vertices are shared by (q,r) and two neighbors.
     * The 6 neighbor directions in Axial are:
     * (+1,0), (+1,-1), (0,-1), (-1,0), (-1,+1), (0,+1)
     */
    private fun getVerticesForHex(center: HexCoord): List<String> {
        val (q, r) = center

        // The 6 standard intersections (triplets of hexes)
        // 1. Top Right: Center, (+1,-1), (+1,0)
        // 2. Right:     Center, (+1,0), (0,+1) ... and so on.

        // Simple iteration of the 6 triangles formed by the center and its neighbors
        val neighbors = listOf(
            HexCoord(q + 1, r - 1), HexCoord(q + 1, r), HexCoord(q, r + 1),
            HexCoord(q - 1, r + 1), HexCoord(q - 1, r), HexCoord(q, r - 1)
        )

        val vertexIds = mutableListOf<String>()

        for (i in 0 until 6) {
            val n1 = neighbors[i]
            val n2 = neighbors[(i + 1) % 6] // Wrap around
            vertexIds.add(HexCoord.getVertexId(center, n1, n2))
        }

        return vertexIds
    }
}

data class HexTile(
    val coordinate: HexCoord,
    val resourceId: String?, // Null = Desert
    val numberToken: Int    // 7 = Desert
)

data class Building(
    val id: String,         // "settlement", "road"
    val ownerId: String,    // "player_123"
    val type: String        // "VERTEX" or "EDGE" (Cached for easy logic)
)

data class Port(
    val locationId: String, // The Edge ID where the port is located
    val resourceId: String?, // Null = 3:1 Generic, "wood" = 2:1 Wood
    val ratio: Int           // 2 or 3
)