package eric.bitria.hexon.game

import eric.bitria.hexon.game.data.HexCoord
import eric.bitria.hexon.game.data.PlacementType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardTest {

    // --- BASICS & SETUP ---

    @Test
    fun `Test Adding Resource Tile`() {
        val board = Board()
        val coord = HexCoord(0, 0)

        board.addTile(coord, "wood", 6)
    }

    @Test
    fun `Test Adding Desert Auto-Sets Robber`() {
        val board = Board()
        val desertCoord = HexCoord(2, -1)

        // Initially at 0,0
        assertEquals(HexCoord(0, 0), board.robberLocation)

        board.addTile(desertCoord, null, 7)

        // Should move to desert
        assertEquals(desertCoord, board.robberLocation)
    }

    @Test
    fun `Test Occupied Check - Basic`() {
        val board = Board()
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)
        val vId = HexCoord.getVertexId(h1, h2, h3)

        board.placeBuilding("settlement", "p1", vId, PlacementType.VERTEX)

        // Try to place on exact same spot
        val result = board.canPlaceBuilding("p2", h1, h2, h3, PlacementType.VERTEX)
        assertFalse(result, "Should not allow building on an occupied vertex")
    }

    // Vertex Logic

    // --- VERTEX PLACEMENT (DISTANCE RULE) ---

    @Test
    fun `Vertex - Simple - Valid Placement on Empty Board`() {
        val board = Board()
        // Vertex at intersection of (0,0), (1,0), (0,1)
        val result = board.canPlaceBuilding("p1", HexCoord(0,0), HexCoord(1,0), HexCoord(0,1), PlacementType.VERTEX)
        assertTrue(result)
    }

    @Test
    fun `Vertex - Constraint - Fail if Neighbor Exists (Directly Adjacent)`() {
        val board = Board()
        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1)

        // We will look at the Vertex (h0, h1, h2)
        val centerVertex = HexCoord.getVertexId(h0, h1, h2)
        board.placeBuilding("settlement", "p1", centerVertex, PlacementType.VERTEX)

        // The neighbor vertex shares edge (h0, h1).
        // The 3rd hex for that neighbor is (1, -1).
        val neighborHex = HexCoord(1, -1)

        val canPlace = board.canPlaceBuilding("p1", h0, h1, neighborHex, PlacementType.VERTEX)
        assertFalse(canPlace, "Cannot place settlement adjacent to existing one")
    }

    @Test
    fun `Vertex - Edge Case - Check All 3 Neighbors`() {
        val board = Board()
        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1)

        val centerVertex = HexCoord.getVertexId(h0, h1, h2)
        board.placeBuilding("settlement", "p1", centerVertex, PlacementType.VERTEX)

        // 1. Neighbor via Edge(h0, h1) -> Third Hex (1, -1)
        assertFalse(board.canPlaceBuilding("p2", h0, h1, HexCoord(1, -1), PlacementType.VERTEX))

        // 2. Neighbor via Edge(h1, h2) -> Third Hex (1, 1)
        assertFalse(board.canPlaceBuilding("p2", h1, h2, HexCoord(1, 1), PlacementType.VERTEX))

        // 3. Neighbor via Edge(h2, h0) -> Third Hex (-1, 1)
        assertFalse(board.canPlaceBuilding("p2", h2, h0, HexCoord(-1, 1), PlacementType.VERTEX))
    }

    @Test
    fun `Vertex - Complex - Sandwich Case`() {
        // Visualize a line of vertices: A -- B -- C -- D
        // If A and C are taken, B is invalid (too close to A and C).
        // But what about D? D is adjacent to C, so invalid.
        // Let's try placing close but not adjacent.

        val board = Board()
        val h0 = HexCoord(0,0)
        val h1 = HexCoord(1,0)
        val h2 = HexCoord(0,1)

        // Place at (h0, h1, h2)
        val v1 = HexCoord.getVertexId(h0, h1, h2)
        board.placeBuilding("settlement", "p1", v1, PlacementType.VERTEX)

        // Neighbor is (h0, h1, 1,-1) [Blocked]
        // Next neighbor along the line is (1,-1, 1,0, 2,-1) [Valid]

        val validFarVertex = HexCoord(1, -1)
        val farHex2 = HexCoord(2, -1)

        // This vertex is Distance=2 from v1. Should be allowed.
        assertTrue(board.canPlaceBuilding("p1", h1, validFarVertex, farHex2, PlacementType.VERTEX))
    }

    // Edge Logic

    // --- EDGE PLACEMENT (ROADS) ---

    @Test
    fun `Edge - Simple - Connect to Own Settlement`() {
        val board = Board()
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        // 1. Place Settlement
        val vId = HexCoord.getVertexId(h1, h2, h3)
        board.placeBuilding("settlement", "p1", vId, PlacementType.VERTEX)

        // 2. Try to place Road on Edge(h1, h2) attached to that settlement
        val canPlace = board.canPlaceBuilding("p1", h1, h2, null, PlacementType.EDGE)
        assertTrue(canPlace)
    }

    @Test
    fun `Edge - Constraint - Cannot Connect to Opponent Settlement`() {
        val board = Board()
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        // 1. Place Opponent Settlement
        val vId = HexCoord.getVertexId(h1, h2, h3)
        board.placeBuilding("settlement", "ENEMY", vId, PlacementType.VERTEX)

        // 2. Try to place my Road
        val canPlace = board.canPlaceBuilding("p1", h1, h2, null, PlacementType.EDGE)
        assertFalse(canPlace)
    }

    @Test
    fun `Edge - Simple - Road Chaining`() {
        val board = Board()
        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1) // Top neighbor

        // Place Base Settlement
        board.placeBuilding("settlement", "p1", HexCoord.getVertexId(h0, h1, h2), PlacementType.VERTEX)

        // Place Road 1 (h0-h1)
        board.placeBuilding("road", "p1", HexCoord.getEdgeId(h0, h1), PlacementType.EDGE)

        // Try Place Road 2 (h1-hNeighbor)
        // h1's neighbors include h0(0,0) and hNew(1,-1)
        // The edge connecting to Road 1 is at vertex (h0, h1, 1,-1)

        val hNext = HexCoord(1, -1)
        assertTrue(board.canPlaceBuilding("p1", h1, hNext, null, PlacementType.EDGE))
    }

    @Test
    fun `Edge - Constraint - Floating Road`() {
        val board = Board()
        // Random edge in the middle of nowhere
        val canPlace = board.canPlaceBuilding("p1", HexCoord(5,5), HexCoord(5,6), null, PlacementType.EDGE)
        assertFalse(canPlace)
    }

    // Production and robber

    // --- PRODUCTION & ROBBER ---

    @Test
    fun `Production - Simple - Settlement Gets 1 Resource`() {
        val board = Board()

        // 1. Use (2,0) to avoid default Robber at (0,0)
        val h1 = HexCoord(2, 0)
        board.addTile(h1, "wood", 6)

        // 2. Place on a valid vertex of (2,0)
        // Neighbors of (2,0) include (3,0) and (2,1)
        val v1 = HexCoord.getVertexId(h1, HexCoord(3, 0), HexCoord(2, 1))

        board.placeBuilding("settlement", "p1", v1, PlacementType.VERTEX)

        val prod = board.getProductionForRoll(6)

        assertEquals(1, prod["p1"]?.get("wood"))
    }

    @Test
    fun `Production - Simple - City Gets 2 Resources`() {
        val board = Board()

        // 1. Use (2,0) to avoid the default Robber at (0,0)
        val h1 = HexCoord(2, 0)
        board.addTile(h1, "ore", 8)

        // 2. Calculate a valid vertex on this new tile
        // Neighbors of (2,0) include (3,0) and (2,1)
        val v1 = HexCoord.getVertexId(h1, HexCoord(3, 0), HexCoord(2, 1))

        board.placeBuilding("city", "p1", v1, PlacementType.VERTEX)

        val prod = board.getProductionForRoll(8)

        assertEquals(2, prod["p1"]?.get("ore"))
    }

    @Test
    fun `Production - Complex - Multiple Players Same Tile`() {
        val board = Board()
        // Use (1,0) to avoid the default Robber at (0,0)
        val tile = HexCoord(1, 0)
        board.addTile(tile, "wheat", 4)

        val v1 = HexCoord.getVertexId(tile, HexCoord(1, -1), HexCoord(2, -1))
        board.placeBuilding("settlement", "p1", v1, PlacementType.VERTEX)

        // 2. Place P2 at "Bottom Left" vertex of (1,0)
        val v2 = HexCoord.getVertexId(tile, HexCoord(0, 1), HexCoord(0, 0))
        board.placeBuilding("settlement", "p2", v2, PlacementType.VERTEX)

        // Roll the dice
        val prod = board.getProductionForRoll(4)

        assertEquals(1, prod["p1"]?.get("wheat"), "Player 1 should get wheat")
        assertEquals(1, prod["p2"]?.get("wheat"), "Player 2 should get wheat")
    }

    @Test
    fun `Production - Robber - Blocks Specific Tile Only`() {
        val board = Board()
        val tileA = HexCoord(0, 0)
        val tileB = HexCoord(2, 0) // Far away but same number

        board.addTile(tileA, "wood", 9)
        board.addTile(tileB, "brick", 9)

        // Build on both
        val vA = HexCoord.getVertexId(tileA, HexCoord(1,0), HexCoord(0,1))
        board.placeBuilding("settlement", "p1", vA, PlacementType.VERTEX)

        val vB = HexCoord.getVertexId(tileB, HexCoord(3,0), HexCoord(2,1))
        board.placeBuilding("settlement", "p1", vB, PlacementType.VERTEX)

        // Move Robber to Tile A
        board.moveRobber(tileA)

        val prod = board.getProductionForRoll(9)

        // Tile A blocked (No Wood)
        val wood = prod["p1"]?.get("wood") ?: 0
        assertEquals(0, wood, "Robbed tile should produce nothing")

        // Tile B active (Brick)
        assertEquals(1, prod["p1"]?.get("brick"), "Unrobbed tile should produce normally")
    }

    @Test
    fun `Robber - Move Logic - Returns Affected Players`() {
        val board = Board()
        val h0 = HexCoord(0, 0)

        // Setup 3 players around h0
        // We need 3 valid vertices spaced out.
        // v1: (0,0), (1,0), (0,1) -> P1
        // v2: (0,0), (-1,1), (-1,0) -> P2
        // v3: (0,0), (0,-1), (1,-1) -> P3

        board.placeBuilding("settlement", "p1", HexCoord.getVertexId(h0, HexCoord(1,0), HexCoord(0,1)), PlacementType.VERTEX)
        board.placeBuilding("settlement", "p2", HexCoord.getVertexId(h0, HexCoord(-1,1), HexCoord(-1,0)), PlacementType.VERTEX)
        board.placeBuilding("settlement", "p3", HexCoord.getVertexId(h0, HexCoord(0,-1), HexCoord(1,-1)), PlacementType.VERTEX)

        val victims = board.moveRobber(h0)

        assertEquals(3, victims.size)
        assertTrue(victims.containsAll(listOf("p1", "p2", "p3")))

        // Move robber to empty tile
        val emptyVictims = board.moveRobber(HexCoord(5,5))
        assertTrue(emptyVictims.isEmpty())
    }
}