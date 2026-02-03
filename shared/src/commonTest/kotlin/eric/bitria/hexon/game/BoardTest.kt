package eric.bitria.hexon.game

import eric.bitria.hexon.game.data.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardTest {

    // Helper to create a board with default rules
    private fun createBoard(): Board {
        val config = GameConfigLoader.default()
        val board = Board()
        board.initialize(config)
        return board
    }

    // New Helper: Adds a small patch of land to allow placement tests to pass.
    // Most tests use (0,0) and its neighbors.
    private fun setupMap(board: Board) {
        board.addTile(HexCoord(0, 0), "wood", 6)
        board.addTile(HexCoord(1, 0), "brick", 8)
        board.addTile(HexCoord(0, 1), "sheep", 4)
        // Add a tile further out for "chaining" and "sandwich" tests
        board.addTile(HexCoord(1, -1), "wheat", 10)
        board.addTile(HexCoord(2, -1), "ore", 3)
    }

    // --- BASICS & SETUP ---

    @Test
    fun `Test Adding Resource Tile`() {
        val board = createBoard()
        val coord = HexCoord(0, 0)

        // "wood" exists in default config
        board.addTile(coord, "wood", 6)

        // Verify internal state implicitly by checking if we can interact with it
        // (Since tiles are private, we can't assert directly without a getter,
        // but no exception means success)
    }

    @Test
    fun `Test Adding Invalid Resource Throws Exception`() {
        val board = createBoard()
        val coord = HexCoord(0, 0)

        assertFailsWith<IllegalArgumentException> {
            board.addTile(coord, "uranium", 6)
        }
    }

    @Test
    fun `Test Adding Desert Auto-Sets Robber`() {
        val board = createBoard()
        val desertCoord = HexCoord(2, -1)

        // Initially at 0,0
        assertEquals(HexCoord(0, 0), board.robberLocation)

        board.addTile(desertCoord, "desert", 7)

        // Should move to desert
        assertEquals(desertCoord, board.robberLocation)
    }

    @Test
    fun `Test Occupied Check - Basic`() {
        val board = createBoard()
        setupMap(board) // Required: Create land

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding("settlement", "p1", h1, h2, h3)

        // Try to place on exact same spot
        val result = board.canPlaceVertexBuilding("p2", h1, h2, h3, "settlement")
        assertFalse(result, "Should not allow building on an occupied vertex")
    }

    // --- VERTEX PLACEMENT (DISTANCE RULE) ---

    @Test
    fun `Vertex - Simple - Valid Placement on Land`() {
        val board = createBoard()
        setupMap(board) // Required: Create land

        // Vertex at intersection of (0,0), (1,0), (0,1) - all exist in setupMap
        val result = board.canPlaceVertexBuilding("p1", HexCoord(0,0), HexCoord(1,0), HexCoord(0,1), "settlement")
        assertTrue(result)
    }

    @Test
    fun `Vertex - Constraint - Fail if Neighbor Exists Directly Adjacent`() {
        val board = createBoard()
        setupMap(board)

        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1)

        // We will look at the Vertex (h0, h1, h2)
        board.placeVertexBuilding("settlement", "p1", h0, h1, h2)

        // The neighbor vertex shares edge (h0, h1).
        // The 3rd hex for that neighbor is (1, -1).
        val neighborHex = HexCoord(1, -1)

        val canPlace = board.canPlaceVertexBuilding("p1", h0, h1, neighborHex, "settlement")
        assertFalse(canPlace, "Cannot place settlement adjacent to existing one")
    }

    @Test
    fun `Vertex - Edge Case - Check All 3 Neighbors`() {
        val board = createBoard()
        setupMap(board)

        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1)

        board.placeVertexBuilding("settlement", "p1", h0, h1, h2)

        // 1. Neighbor via Edge(h0, h1) -> Third Hex (1, -1)
        assertFalse(board.canPlaceVertexBuilding("p2", h0, h1, HexCoord(1, -1), "settlement"))

        // 2. Neighbor via Edge(h1, h2) -> Third Hex (1, 1)
        // Note: Hex(1,1) isn't in setupMap, but h1 and h2 are.
        // hasTileConnection returns true if ANY hex is present.
        // Since h1 and h2 are present, this is "Coastal" placement, so validity check proceeds to distance rule.
        assertFalse(board.canPlaceVertexBuilding("p2", h1, h2, HexCoord(1, 1), "settlement"))

        // 3. Neighbor via Edge(h2, h0) -> Third Hex (-1, 1)
        assertFalse(board.canPlaceVertexBuilding("p2", h2, h0, HexCoord(-1, 1), "settlement"))
    }

    @Test
    fun `Vertex - Complex - Sandwich Case`() {
        val board = createBoard()
        setupMap(board)

        val h0 = HexCoord(0,0)
        val h1 = HexCoord(1,0)
        val h2 = HexCoord(0,1)

        // Place at (h0, h1, h2)
        board.placeVertexBuilding("settlement", "p1", h0, h1, h2)

        // Neighbor is (h0, h1, 1,-1) [Blocked]
        // Next neighbor along the line is (1,-1, 1,0, 2,-1) [Valid]
        // setupMap includes (1,-1) and (2,-1), so this location is valid land.

        val validFarVertex = HexCoord(1, -1)
        val farHex2 = HexCoord(2, -1)

        // This vertex is Distance=2 from v1. Should be allowed.
        assertTrue(board.canPlaceVertexBuilding("p1", h1, validFarVertex, farHex2, "settlement"))
    }

    // --- UPGRADE LOGIC ---

    @Test
    fun `Vertex - Upgrade - Settlement to City`() {
        val board = createBoard()
        setupMap(board)

        val h1 = HexCoord(0,0); val h2 = HexCoord(1,0); val h3 = HexCoord(0,1)

        // 1. Place Settlement
        board.placeVertexBuilding("settlement", "p1", h1, h2, h3)

        // 2. Check if we can build a City (Upgrade)
        assertTrue(board.canPlaceVertexBuilding("p1", h1, h2, h3, "city"))

        // 3. Perform Upgrade
        val success = board.placeVertexBuilding("city", "p1", h1, h2, h3)
        assertTrue(success)

        // 4. Verify ID changed
        val vId = HexCoord.getVertexId(h1, h2, h3)
        assertEquals("city", board.getBuildingAt(vId)?.def?.id)
    }

    @Test
    fun `Vertex - Upgrade - Fail if Different Owner`() {
        val board = createBoard()
        setupMap(board)

        val h1 = HexCoord(0,0); val h2 = HexCoord(1,0); val h3 = HexCoord(0,1)

        board.placeVertexBuilding("settlement", "p1", h1, h2, h3)

        // Player 2 tries to upgrade Player 1's settlement
        assertFalse(board.canPlaceVertexBuilding("p2", h1, h2, h3, "city"))
        assertFalse(board.placeVertexBuilding("city", "p2", h1, h2, h3))
    }

    // --- EDGE PLACEMENT (ROADS) ---

    @Test
    fun `Edge - Simple - Connect to Own Settlement`() {
        val board = createBoard()
        setupMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        // 1. Place Settlement
        board.placeVertexBuilding("settlement", "p1", h1, h2, h3)

        // 2. Try to place Road on Edge(h1, h2) attached to that settlement
        val canPlace = board.canPlaceEdgeBuilding("p1", h1, h2, "road")
        assertTrue(canPlace)
    }

    @Test
    fun `Edge - Constraint - Cannot Connect to Opponent Settlement`() {
        val board = createBoard()
        setupMap(board)

        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        // 1. Place Opponent Settlement
        board.placeVertexBuilding("settlement", "ENEMY", h1, h2, h3)

        // 2. Try to place my Road
        val canPlace = board.canPlaceEdgeBuilding("p1", h1, h2, "road")
        assertFalse(canPlace)
    }

    @Test
    fun `Edge - Simple - Road Chaining`() {
        val board = createBoard()
        setupMap(board)

        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1)

        // Place Base Settlement
        board.placeVertexBuilding("settlement", "p1", h0, h1, h2)

        // Place Road 1 (h0-h1)
        board.placeEdgeBuilding("road", "p1", h0, h1)

        // Place Road 2 (h1 - hNext)
        // h1 is (1,0), hNext is (1,-1). Both are in setupMap.
        val hNext = HexCoord(1, -1)
        assertTrue(board.canPlaceEdgeBuilding("p1", h1, hNext, "road"))
    }

    @Test
    fun `Edge - Constraint - Floating Road (Network Logic)`() {
        val board = createBoard()
        setupMap(board)

        // We pick an edge that exists on the map but has no buildings/roads nearby.
        // (1,-1) and (2,-1) are in setupMap, so this is valid land.
        val hA = HexCoord(1, -1)
        val hB = HexCoord(2, -1)

        val canPlace = board.canPlaceEdgeBuilding("p1", hA, hB, "road")

        // Should fail because it's not connected to a settlement or road
        assertFalse(canPlace)
    }

    @Test
    fun `Edge - Constraint - Floating Road (Open Sea Logic)`() {
        val board = createBoard()
        // We DO NOT call setupMap here, or we pick coords far away.

        // Open sea coords
        val canPlace = board.canPlaceEdgeBuilding("p1", HexCoord(50,50), HexCoord(50,51), "road")

        // Should fail because no tiles exist there
        assertFalse(canPlace)
    }

    // --- PRODUCTION & ROBBER ---

    @Test
    fun `Production - Simple - Settlement Gets 1 Resource`() {
        val board = createBoard()

        // 1. Manually add tile (instead of setupMap) to control resource type
        val h1 = HexCoord(2, 0)
        board.addTile(h1, "wood", 6)

        // 2. Place on a valid vertex of (2,0).
        // Note: For hasTileConnection to pass, at least one hex must exist. h1 exists.
        val v1NeighA = HexCoord(3, 0)
        val v1NeighB = HexCoord(2, 1)

        board.placeVertexBuilding("settlement", "p1", h1, v1NeighA, v1NeighB)

        val prod = board.getProductionForRoll(6)

        assertEquals(1, prod["p1"]?.get("wood"))
    }

    @Test
    fun `Production - Simple - City Gets 2 Resources`() {
        val board = createBoard()

        val h1 = HexCoord(2, 0)
        board.addTile(h1, "ore", 8)

        val v1NeighA = HexCoord(3, 0)
        val v1NeighB = HexCoord(2, 1)

        board.placeVertexBuilding("settlement", "p1", h1, v1NeighA, v1NeighB)
        board.placeVertexBuilding("city", "p1", h1, v1NeighA, v1NeighB)

        val prod = board.getProductionForRoll(8)

        assertEquals(2, prod["p1"]?.get("ore"))
    }

    @Test
    fun `Production - Complex - Multiple Players Same Tile`() {
        val board = createBoard()
        val tile = HexCoord(1, 0)
        board.addTile(tile, "wheat", 4)

        // P1 Top Right
        board.placeVertexBuilding("settlement", "p1", tile, HexCoord(1, -1), HexCoord(2, -1))

        // P2 Bottom Left
        board.placeVertexBuilding("settlement", "p2", tile, HexCoord(0, 1), HexCoord(0, 0))

        val prod = board.getProductionForRoll(4)

        assertEquals(1, prod["p1"]?.get("wheat"), "Player 1 should get wheat")
        assertEquals(1, prod["p2"]?.get("wheat"), "Player 2 should get wheat")
    }

    @Test
    fun `Production - Robber - Blocks Specific Tile Only`() {
        val board = createBoard()
        val tileA = HexCoord(0, 0)
        val tileB = HexCoord(2, 0)

        board.addTile(tileA, "wood", 9)
        board.addTile(tileB, "brick", 9)

        board.placeVertexBuilding("settlement", "p1", tileA, HexCoord(1,0), HexCoord(0,1))
        board.placeVertexBuilding("settlement", "p1", tileB, HexCoord(3,0), HexCoord(2,1))

        board.moveRobber(tileA)

        val prod = board.getProductionForRoll(9)

        val wood = prod["p1"]?.get("wood") ?: 0
        assertEquals(0, wood, "Robbed tile should produce nothing")

        assertEquals(1, prod["p1"]?.get("brick"), "Unrobbed tile should produce normally")
    }

    @Test
    fun `Robber - Move Logic - Returns Affected Players`() {
        val board = createBoard()
        val h0 = HexCoord(0, 0)
        board.addTile(h0, "wood", 6) // Need tile to place buildings validly

        // Setup 3 players around h0
        board.placeVertexBuilding("settlement", "p1", h0, HexCoord(1,0), HexCoord(0,1))
        board.placeVertexBuilding("settlement", "p2", h0, HexCoord(-1,1), HexCoord(-1,0))
        board.placeVertexBuilding("settlement", "p3", h0, HexCoord(0,-1), HexCoord(1,-1))

        val victims = board.moveRobber(h0)

        assertEquals(3, victims.size)
        assertTrue(victims.containsAll(listOf("p1", "p2", "p3")))
    }
}