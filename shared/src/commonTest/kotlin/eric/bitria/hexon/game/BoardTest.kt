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
        return Board(config.resources, config.buildings)
    }

    // --- BASICS & SETUP ---

    @Test
    fun `Test Adding Resource Tile`() {
        val board = createBoard()
        val coord = HexCoord(0, 0)

        // "wood" exists in default config
        board.addTile(coord, "wood", 6)
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

        board.addTile(desertCoord, null, 7)

        // Should move to desert
        assertEquals(desertCoord, board.robberLocation)
    }

    @Test
    fun `Test Occupied Check - Basic`() {
        val board = createBoard()
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        board.placeVertexBuilding("settlement", "p1", h1, h2, h3)

        // Try to place on exact same spot (with same type, effectively a "re-build" which is invalid if same owner, or blocked if diff owner)
        val result = board.canPlaceVertexBuilding("p2", h1, h2, h3, "settlement")
        assertFalse(result, "Should not allow building on an occupied vertex")
    }

    // --- VERTEX PLACEMENT (DISTANCE RULE) ---

    @Test
    fun `Vertex - Simple - Valid Placement on Empty Board`() {
        val board = createBoard()
        // Vertex at intersection of (0,0), (1,0), (0,1)
        val result = board.canPlaceVertexBuilding("p1", HexCoord(0,0), HexCoord(1,0), HexCoord(0,1), "settlement")
        assertTrue(result)
    }

    @Test
    fun `Vertex - Constraint - Fail if Neighbor Exists Directly Adjacent`() {
        val board = createBoard()
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
        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1)

        board.placeVertexBuilding("settlement", "p1", h0, h1, h2)

        // 1. Neighbor via Edge(h0, h1) -> Third Hex (1, -1)
        assertFalse(board.canPlaceVertexBuilding("p2", h0, h1, HexCoord(1, -1), "settlement"))

        // 2. Neighbor via Edge(h1, h2) -> Third Hex (1, 1)
        assertFalse(board.canPlaceVertexBuilding("p2", h1, h2, HexCoord(1, 1), "settlement"))

        // 3. Neighbor via Edge(h2, h0) -> Third Hex (-1, 1)
        assertFalse(board.canPlaceVertexBuilding("p2", h2, h0, HexCoord(-1, 1), "settlement"))
    }

    @Test
    fun `Vertex - Complex - Sandwich Case`() {
        val board = createBoard()
        val h0 = HexCoord(0,0)
        val h1 = HexCoord(1,0)
        val h2 = HexCoord(0,1)

        // Place at (h0, h1, h2)
        board.placeVertexBuilding("settlement", "p1", h0, h1, h2)

        // Neighbor is (h0, h1, 1,-1) [Blocked]
        // Next neighbor along the line is (1,-1, 1,0, 2,-1) [Valid]

        val validFarVertex = HexCoord(1, -1)
        val farHex2 = HexCoord(2, -1)

        // This vertex is Distance=2 from v1. Should be allowed.
        assertTrue(board.canPlaceVertexBuilding("p1", h1, validFarVertex, farHex2, "settlement"))
    }

    // --- UPGRADE LOGIC ---

    @Test
    fun `Vertex - Upgrade - Settlement to City`() {
        val board = createBoard()
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
        val h0 = HexCoord(0, 0)
        val h1 = HexCoord(1, 0)
        val h2 = HexCoord(0, 1) // Top neighbor

        // Place Base Settlement
        board.placeVertexBuilding("settlement", "p1", h0, h1, h2)

        // Place Road 1 (h0-h1)
        board.placeEdgeBuilding("road", "p1", h0, h1)

        // Try Place Road 2 (h1-hNeighbor)
        // h1's neighbors include h0(0,0) and hNew(1,-1)
        val hNext = HexCoord(1, -1)
        assertTrue(board.canPlaceEdgeBuilding("p1", h1, hNext, "road"))
    }

    @Test
    fun `Edge - Constraint - Floating Road`() {
        val board = createBoard()
        // Random edge in the middle of nowhere
        val canPlace = board.canPlaceEdgeBuilding("p1", HexCoord(5,5), HexCoord(5,6), "road")
        assertFalse(canPlace)
    }

    // --- PRODUCTION & ROBBER ---

    @Test
    fun `Production - Simple - Settlement Gets 1 Resource`() {
        val board = createBoard()

        // 1. Use (2,0) to avoid default Robber at (0,0)
        val h1 = HexCoord(2, 0)
        board.addTile(h1, "wood", 6)

        // 2. Place on a valid vertex of (2,0)
        val v1NeighA = HexCoord(3, 0)
        val v1NeighB = HexCoord(2, 1)

        board.placeVertexBuilding("settlement", "p1", h1, v1NeighA, v1NeighB)

        val prod = board.getProductionForRoll(6)

        assertEquals(1, prod["p1"]?.get("wood"))
    }

    @Test
    fun `Production - Simple - City Gets 2 Resources`() {
        val board = createBoard()

        // 1. Use (2,0) to avoid the default Robber at (0,0)
        val h1 = HexCoord(2, 0)
        board.addTile(h1, "ore", 8)

        val v1NeighA = HexCoord(3, 0)
        val v1NeighB = HexCoord(2, 1)

        // Place City directly (works in setup, or if we bypass logic, but let's assume valid placement for test)
        // Or place settlement then upgrade
        board.placeVertexBuilding("settlement", "p1", h1, v1NeighA, v1NeighB)
        board.placeVertexBuilding("city", "p1", h1, v1NeighA, v1NeighB)

        val prod = board.getProductionForRoll(8)

        // City production defined as 2 in Default Config
        assertEquals(2, prod["p1"]?.get("ore"))
    }

    @Test
    fun `Production - Complex - Multiple Players Same Tile`() {
        val board = createBoard()
        // Use (1,0) to avoid the default Robber at (0,0)
        val tile = HexCoord(1, 0)
        board.addTile(tile, "wheat", 4)

        // P1 Top Right
        board.placeVertexBuilding("settlement", "p1", tile, HexCoord(1, -1), HexCoord(2, -1))

        // P2 Bottom Left
        board.placeVertexBuilding("settlement", "p2", tile, HexCoord(0, 1), HexCoord(0, 0))

        // Roll the dice
        val prod = board.getProductionForRoll(4)

        assertEquals(1, prod["p1"]?.get("wheat"), "Player 1 should get wheat")
        assertEquals(1, prod["p2"]?.get("wheat"), "Player 2 should get wheat")
    }

    @Test
    fun `Production - Robber - Blocks Specific Tile Only`() {
        val board = createBoard()
        val tileA = HexCoord(0, 0)
        val tileB = HexCoord(2, 0) // Far away but same number

        board.addTile(tileA, "wood", 9)
        board.addTile(tileB, "brick", 9)

        // Build on both
        board.placeVertexBuilding("settlement", "p1", tileA, HexCoord(1,0), HexCoord(0,1))
        board.placeVertexBuilding("settlement", "p1", tileB, HexCoord(3,0), HexCoord(2,1))

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
        val board = createBoard()
        val h0 = HexCoord(0, 0)

        // Setup 3 players around h0
        board.placeVertexBuilding("settlement", "p1", h0, HexCoord(1,0), HexCoord(0,1))
        board.placeVertexBuilding("settlement", "p2", h0, HexCoord(-1,1), HexCoord(-1,0))
        board.placeVertexBuilding("settlement", "p3", h0, HexCoord(0,-1), HexCoord(1,-1))

        val victims = board.moveRobber(h0)

        assertEquals(3, victims.size)
        assertTrue(victims.containsAll(listOf("p1", "p2", "p3")))

        // Move robber to empty tile
        val emptyVictims = board.moveRobber(HexCoord(5,5))
        assertTrue(emptyVictims.isEmpty())
    }

    // --- VALIDATION & CONSTRAINTS ---

    @Test
    fun `Validation - Throws if Building Type Does Not Exist`() {
        val board = createBoard()
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        // 1. Vertex Placement with unknown ID
        val exceptionVertex = assertFailsWith<IllegalArgumentException> {
            board.placeVertexBuilding("skyscraper", "p1", h1, h2, h3)
        }
        assertTrue(exceptionVertex.message!!.contains("not defined"))

        // 2. Edge Placement with unknown ID
        val exceptionEdge = assertFailsWith<IllegalArgumentException> {
            board.placeEdgeBuilding("highway", "p1", h1, h2)
        }
        assertTrue(exceptionEdge.message!!.contains("not defined"))
    }

    @Test
    fun `Validation - Throws if Placement Type Mismatch`() {
        val board = createBoard()
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        // 1. Try to place a ROAD (Edge Type) using placeVertexBuilding
        // "road" is defined as PlacementType.EDGE in default config
        val exceptionVertex = assertFailsWith<IllegalArgumentException> {
            board.placeVertexBuilding("road", "p1", h1, h2, h3)
        }
        assertTrue(exceptionVertex.message!!.contains("not a VERTEX building"))

        // 2. Try to place a SETTLEMENT (Vertex Type) using placeEdgeBuilding
        // "settlement" is defined as PlacementType.VERTEX in default config
        val exceptionEdge = assertFailsWith<IllegalArgumentException> {
            board.placeEdgeBuilding("settlement", "p1", h1, h2)
        }
        assertTrue(exceptionEdge.message!!.contains("not an EDGE building"))
    }

    @Test
    fun `Validation - CanPlace Checks Also Validate ID`() {
        val board = createBoard()
        val h1 = HexCoord(0, 0)
        val h2 = HexCoord(1, 0)
        val h3 = HexCoord(0, 1)

        // Even checking "canPlace" should throw if the building ID is nonsense
        // This ensures the UI/GameEngine catches typos early
        assertFailsWith<IllegalArgumentException> {
            board.canPlaceVertexBuilding("p1", h1, h2, h3, "magic_tower")
        }

        assertFailsWith<IllegalArgumentException> {
            board.canPlaceEdgeBuilding("p1", h1, h2, "teleporter")
        }
    }
}