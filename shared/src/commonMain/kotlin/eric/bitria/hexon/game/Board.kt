package eric.bitria.hexon.game

import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.PlacementType
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.PortVertex
import eric.bitria.hexon.game.data.ResourceId


class Board {

    // --- State Storage ---
    // Key: "q,r"
    private val tiles = mutableMapOf<String, HexTile>()

    // Key: Normalized ID (Edge or Vertex string)
    // Value: The building placed there
    private val buildings = mutableMapOf<String, Building>()

    // Key: Normalized Vertex ID
    private val ports = mutableMapOf<PortVertex, Port>()

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
    fun placeBuilding(typeId: BuildingId, ownerId: String, locId: String, type: PlacementType): Boolean {
        if (buildings.containsKey(locId)) return false

        buildings[locId] = Building(
            id = typeId,
            ownerId = ownerId,
            type = type
        )
        return true
    }

    fun getBuildingAt(locId: BuildingId): Building? = buildings[locId]

    // --- Production Logic (The Core) ---

    /**
     * Given a dice roll, calculate who gets what.
     * @return Map of PlayerID -> List of ResourceIDs
     */
    fun getProductionForRoll(roll: Int): Map<String, MutableMap<ResourceId, Int>> {
        val production = mutableMapOf<String, MutableMap<ResourceId, Int>>()

        // 1. Find all tiles matching the dice roll and not blocked by the robber
        val activeTiles = tiles.values.filter {
            it.numberToken == roll && it.coordinate != robberLocation
        }

        // 2. For each active tile, check its 6 vertices
        for (tile in activeTiles) {
            val resource = tile.resourceId ?: continue
            val vertices = getVerticesForHex(tile.coordinate)

            for (vertexId in vertices) {
                val building = buildings[vertexId] ?: continue

                // Settlement = 1, City = 2
                val amount = if (building.id == "city") 2 else 1

                val playerProduction =
                    production.getOrPut(building.ownerId) { mutableMapOf() }

                playerProduction[resource] =
                    (playerProduction[resource] ?: 0) + amount
            }
        }

        return production
    }

    // --- Helpers ---

    /**
     * Moves the robber to the selected HexCoord.
     * @return A list of the affected players.
     */
    fun moveRobber(target: HexCoord): List<PlayerId> {
        robberLocation = target
        return getVerticesForHex(target)
            .mapNotNull { buildings[it]?.ownerId }
            .distinct()
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
    val ownerId: PlayerId,    // "player_123"
    val type: PlacementType // "VERTEX" or "EDGE"
)

data class Port(
    val locationId: PortVertex, // The Vertex ID where the port is located
    val resourceId: String?, // Null = 3:1 Generic, "wood" = 2:1 Wood
    val ratio: Int           // 2 or 3
)