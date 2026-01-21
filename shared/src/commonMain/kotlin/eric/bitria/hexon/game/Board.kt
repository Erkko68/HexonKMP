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

    /**
     * @return The building at the given location ID, or null if none
     */
    fun getBuildingAt(locId: BuildingId): Building? = buildings[locId]

    /**
     * @return true if the spot is empty and can be placed
     */
    fun canPlaceBuilding(ownerId: String, hexA: HexCoord, hexB: HexCoord, hexC: HexCoord?, type: PlacementType): Boolean {
        val locId = when (type) {
            PlacementType.VERTEX -> {
                if (hexC == null) return false
                HexCoord.getVertexId(hexA, hexB, hexC)
            }
            PlacementType.EDGE -> HexCoord.getEdgeId(hexA, hexB)
        }

        if (buildings.containsKey(locId)) return false

        return when (type) {
            PlacementType.VERTEX -> {
                if (hexC == null) return false
                checkVertexPlacement(hexA, hexB, hexC)
            }
            PlacementType.EDGE -> {
                checkEdgePlacement(hexA, hexB, ownerId)
            }
        }
    }

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

    // --- Helpers ---

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

    private fun checkVertexPlacement(h1: HexCoord, h2: HexCoord, h3: HexCoord): Boolean {
        // Rule: Distance Rule. No adjacent vertex can have a building.
        // The 3 neighbors of Vertex(h1,h2,h3) are the other ends of the 3 edges meeting here.
        // Edge 1: Shared by h1-h2. The "other" neighbor determines the adjacent vertex.

        val neighbors = listOf(
            getNeighborVertex(h1, h2, h3), // Neighbor across edge h1-h2
            getNeighborVertex(h2, h3, h1), // Neighbor across edge h2-h3
            getNeighborVertex(h3, h1, h2)  // Neighbor across edge h3-h1
        )

        // If ANY neighbor has a building, return false
        return neighbors.none { buildings.containsKey(it) }
    }

    private fun checkEdgePlacement(h1: HexCoord, h2: HexCoord, ownerId: String): Boolean {
        // Rule: Must connect to a building or road owned by the same player.

        // 1. Find the two endpoints (Vertices) of this edge (h1-h2)
        val commonNeighbors = getCommonNeighbors(h1, h2)
        if (commonNeighbors.size != 2) return false // Should be impossible on valid grid

        val endpoint1 = HexCoord.getVertexId(h1, h2, commonNeighbors[0])
        val endpoint2 = HexCoord.getVertexId(h1, h2, commonNeighbors[1])

        // 2. Check connectivity at either endpoint
        return isConnectedAtVertex(endpoint1, ownerId, h1, h2, commonNeighbors[0]) ||
                isConnectedAtVertex(endpoint2, ownerId, h1, h2, commonNeighbors[1])
    }

    // --- Geometry Helpers ---

    /**
     * Given a vertex composed of Hexes A, B, and C.
     * We want to find the adjacent vertex connected via the edge shared by A and B.
     * Logic: A and B share two vertices. One is (A,B,C). The other is (A,B,X).
     * We need to find X.
     */
    private fun getNeighborVertex(a: HexCoord, b: HexCoord, currentThird: HexCoord): String {
        // Get all common neighbors of A and B
        val common = getCommonNeighbors(a, b)
        // One of them is 'currentThird', we want the other one.
        val other = common.firstOrNull { it != currentThird } ?: return ""
        return HexCoord.getVertexId(a, b, other)
    }

    private fun isConnectedAtVertex(
        vertexId: String,
        ownerId: String,
        h1: HexCoord, h2: HexCoord, h3: HexCoord // The 3 hexes making this vertex
    ): Boolean {
        // 1. Owns the building at this vertex?
        val building = buildings[vertexId]
        if (building != null && building.ownerId == ownerId) return true

        // 2. Owns an adjacent road?
        // The edges connected to vertex(h1,h2,h3) are (h1,h3) and (h2,h3).
        // (We exclude h1,h2 because that's the edge we are currently trying to place)
        val edgeA = HexCoord.getEdgeId(h1, h3)
        val edgeB = HexCoord.getEdgeId(h2, h3)

        return (buildings[edgeA]?.ownerId == ownerId) || (buildings[edgeB]?.ownerId == ownerId)
    }

    private fun getCommonNeighbors(h1: HexCoord, h2: HexCoord): List<HexCoord> {
        val n1 = getNeighbors(h1) // Util from previous step
        val n2 = getNeighbors(h2)
        return n1.intersect(n2.toSet()).toList()
    }

    // Re-included for context if you didn't add it yet
    private fun getNeighbors(center: HexCoord): List<HexCoord> {
        val (q, r) = center
        return listOf(
            HexCoord(q + 1, r - 1), HexCoord(q + 1, r), HexCoord(q, r + 1),
            HexCoord(q - 1, r + 1), HexCoord(q - 1, r), HexCoord(q, r - 1)
        )
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