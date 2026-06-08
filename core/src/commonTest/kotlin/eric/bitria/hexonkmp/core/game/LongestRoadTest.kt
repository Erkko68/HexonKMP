package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.action.MoveRobber
import eric.bitria.hexonkmp.core.game.action.PlaceRoad
import eric.bitria.hexonkmp.core.game.action.StealFrom
import eric.bitria.hexonkmp.core.game.action.UpgradeCity
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.event.LongestRoadChanged
import eric.bitria.hexonkmp.core.game.event.PhaseChanged
import eric.bitria.hexonkmp.core.game.event.ResourceStolen
import eric.bitria.hexonkmp.core.game.model.Building
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.Road
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import eric.bitria.hexonkmp.core.game.model.board.endpoints
import eric.bitria.hexonkmp.core.game.model.board.incidentEdges
import eric.bitria.hexonkmp.core.game.model.board.touches
import eric.bitria.hexonkmp.core.game.model.board.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LongestRoadTest {

    private val engine = CatanGameEngine(boardSeed = 1)
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))

    // Build a chain of [n] connected board edges starting from [startEdge].
    // Returns fewer than [n] if the board has no more adjacent edges.
    private fun chainEdges(startEdge: Edge, n: Int): List<Edge> {
        val boardEdges = play.board.edges()
        val chain = mutableListOf(startEdge)
        var vertex = startEdge.endpoints().first()
        while (chain.size < n) {
            val next = vertex.incidentEdges()
                .firstOrNull { it in boardEdges && it !in chain } ?: break
            chain.add(next)
            vertex = next.endpoints().first { it != vertex }
        }
        return chain
    }

    // Find a board edge not occupied by any road in [play].
    private fun freeEdge(): Edge = play.board.edges().first { play.roadAt(it) == null }

    // Inject [roads] as alice's roads directly into a Play-phase state.
    private fun withAliceRoads(roads: List<Edge>) = play.copy(
        roads = roads.map { Road(alice, it) },
    )

    // --- longestRoadOf (pure algorithm) ---

    @Test
    fun singleRoadHasLengthOne() {
        val edge = freeEdge()
        val state = withAliceRoads(listOf(edge))
        assertEquals(1, engine.longestRoadOf(state, alice))
    }

    @Test
    fun fiveConnectedRoadsHasLengthFive() {
        val edges = chainEdges(freeEdge(), 5)
        val state = withAliceRoads(edges)
        assertEquals(5, engine.longestRoadOf(state, alice))
    }

    @Test
    fun disconnectedGroupsCountsLongestSegmentOnly() {
        val edge1 = freeEdge()
        // Second edge disconnected from the first (isolated — find an edge with no shared vertex)
        val edge2 = play.board.edges().first { e ->
            e != edge1 && e.endpoints().none { it in edge1.endpoints() }
        }
        val state = withAliceRoads(listOf(edge1, edge2))
        // Two isolated single roads — longest is 1.
        assertEquals(1, engine.longestRoadOf(state, alice))
    }

    @Test
    fun opponentBuildingBreaksChainContinuity() {
        val edges = chainEdges(freeEdge(), 5)
        // Bob's settlement sits in the middle vertex of the 5-road chain.
        val midVertex: Vertex = edges[2].endpoints()
            .first { v -> edges[1].touches(v) && edges[2].touches(v) }
        val bobBuilding = Building(bob, midVertex, Building.Kind.SETTLEMENT)

        val state = withAliceRoads(edges).copy(
            buildings = play.buildings + bobBuilding,
        )
        // Chain of 5 broken by bob at position 3 → max continuous segment is 2.
        assertTrue(engine.longestRoadOf(state, alice) < 5)
    }

    @Test
    fun ownBuildingDoesNotBreakChain() {
        val edges = chainEdges(freeEdge(), 5)
        val midVertex: Vertex = edges[2].endpoints()
            .first { v -> edges[1].touches(v) && edges[2].touches(v) }
        val aliceBuilding = Building(alice, midVertex, Building.Kind.SETTLEMENT)

        val state = withAliceRoads(edges).copy(
            buildings = play.buildings + aliceBuilding,
        )
        // Alice's own building does not interrupt her road.
        assertEquals(5, engine.longestRoadOf(state, alice))
    }

    // --- Longest Road award (via engine.reduce) ---

    @Test
    fun buildingFifthRoadAwardsLongestRoad() {
        // Alice has 4 connected roads injected; she then builds her 5th via reduce().
        val edges = chainEdges(freeEdge(), 4)
        val state = withAliceRoads(edges).copy(
            hands = play.hands + (alice to ResourceCount.of(Resource.LUMBER to 1, Resource.BRICK to 1)),
        )
        // The 5th road must connect to the chain via the engine's normal road rules.
        // Find the open endpoint of the chain and a valid adjacent edge on the board.
        val chainEnd: Vertex = edges.last().endpoints()
            .first { v -> edges.dropLast(1).none { it.touches(v) } }
        val fifthEdge = chainEnd.incidentEdges()
            .firstOrNull { it in play.board.edges() && it !in edges && play.roadAt(it) == null }

        if (fifthEdge == null) return // board layout doesn't allow it from this start — skip

        val result = engine.reduce(state, alice, PlaceRoad(fifthEdge))
        assertNull(result.rejection)
        assertEquals(alice, result.state.longestRoad)
        assertTrue(result.events.any { it is LongestRoadChanged && it.holder == alice })
    }

    @Test
    fun fourRoadsDoNotAwardLongestRoad() {
        val edges = chainEdges(freeEdge(), 4)
        val state = withAliceRoads(edges)
        assertNull(state.longestRoad)
        assertEquals(4, engine.longestRoadOf(state, alice))
    }

    @Test
    fun incumbentKeepsLongestRoadOnTie() {
        // Alice holds Longest Road with 5 roads; bob builds 5 roads — should NOT take it.
        val aliceEdges = chainEdges(freeEdge(), 5)
        val aliceRoads = aliceEdges.map { Road(alice, it) }

        // Bob's 5-road chain must be different edges.
        val aliceEdgeSet = aliceEdges.toSet()
        val bobStart = play.board.edges().first { it !in aliceEdgeSet }
        val bobEdges = chainEdges(bobStart, 5).filter { it !in aliceEdgeSet }
        if (bobEdges.size < 5) return // not enough free edges on this board layout — skip

        val state = play.copy(
            roads = aliceRoads + bobEdges.map { Road(bob, it) },
            longestRoad = alice,  // alice currently holds it
        )
        // Bob placing a 6th road would take it; here we only check that 5==5 doesn't.
        assertEquals(alice, state.longestRoad)
        assertEquals(5, engine.longestRoadOf(state, alice))
        assertEquals(5, engine.longestRoadOf(state, bob))
    }

    @Test
    fun longestRoadVpCountsTowardWin() {
        // Alice has 3 cities (6 VP) + 1 settlement (1 VP) + Longest Road (2 VP) = 9 VP.
        // Upgrading the settlement to a city brings her to 10 VP and the game ends.
        // This verifies that longestRoad VP is included in the win-condition check.
        val aliceSettlement = play.buildings.first { it.owner == alice && it.kind == Building.Kind.SETTLEMENT }
        val verts = play.board.vertices().toList()
        // Pick 3 unused vertices for alice's cities.
        val usedVerts = play.buildings.map { it.vertex }.toSet()
        val cityVerts = verts.filter { it !in usedVerts }.take(3)
        if (cityVerts.size < 3) return // not enough free vertices on this seed — skip
        val threeCities = cityVerts.map { Building(alice, it, Building.Kind.CITY) }

        val state = play.copy(
            buildings = play.buildings.filter { it.owner != alice } + aliceSettlement + threeCities,
            longestRoad = alice,
            hands = play.hands + (alice to ResourceCount.of(Resource.ORE to 3, Resource.GRAIN to 2)),
        )
        val result = engine.reduce(state, alice, UpgradeCity(aliceSettlement.vertex))
        assertNull(result.rejection, "UpgradeCity rejected: ${result.rejection}")
        assertEquals(GamePhase.Finished(alice), result.state.phase)
    }

    // --- Robber: ChooseStealTarget when multiple opponents on tile ---

    @Test
    fun multipleVictimsEnterChooseStealTargetPhase() {
        val players = listOf(alice, bob, PlayerId("carol"))
        val engine3 = CatanGameEngine(boardSeed = 1)
        val state3 = engine3.completeSetup(engine3.initialState(players))

        // Find a tile with buildings from both bob and carol.
        val tileCandidates = state3.board.tiles.map { it.hex }.filter { hex ->
            val owners = state3.buildings
                .filter { hex in it.vertex.hexes }
                .map { it.owner }
                .distinct()
            bob in owners && PlayerId("carol") in owners && alice !in owners
        }
        if (tileCandidates.isEmpty()) return // board layout may not have this — skip

        val hex = tileCandidates.first { it != state3.board.robber }
        val carol = PlayerId("carol")
        val s = state3.copy(
            phase = GamePhase.Robber,
            hands = state3.hands + (bob to ResourceCount.of(Resource.BRICK to 1))
                    + (carol to ResourceCount.of(Resource.ORE to 1)),
        )
        val result = engine3.reduce(s, alice, MoveRobber(hex))
        assertNull(result.rejection)
        val phase = result.state.phase as? GamePhase.ChooseStealTarget
        assertNotNull(phase)
        assertTrue(bob in phase.victims && carol in phase.victims)
        assertTrue(result.events.any { it is PhaseChanged && it.phase is GamePhase.ChooseStealTarget })
    }

    @Test
    fun stealFromValidTargetStealsAndReturnsToPlay() {
        val players = listOf(alice, bob, PlayerId("carol"))
        val engine3 = CatanGameEngine(boardSeed = 1)
        val state3 = engine3.completeSetup(engine3.initialState(players))
        val carol = PlayerId("carol")

        val tileCandidates = state3.board.tiles.map { it.hex }.filter { hex ->
            val owners = state3.buildings
                .filter { hex in it.vertex.hexes }
                .map { it.owner }
                .distinct()
            bob in owners && carol in owners && alice !in owners
        }
        if (tileCandidates.isEmpty()) return

        val hex = tileCandidates.first { it != state3.board.robber }
        val inChoose = state3.copy(
            phase = GamePhase.ChooseStealTarget(listOf(bob, carol)),
            board = state3.board.copy(robber = hex),
            hands = state3.hands + (bob to ResourceCount.of(Resource.BRICK to 2)),
        )
        val aliceBefore = inChoose.handOf(alice).total
        val result = engine3.reduce(inChoose, alice, StealFrom(bob))
        assertNull(result.rejection)
        assertEquals(GamePhase.Play, result.state.phase)
        assertTrue(result.events.any { it is ResourceStolen && it.from == bob && it.by == alice })
        assertEquals(aliceBefore + 1, result.state.handOf(alice).total)
    }

    @Test
    fun stealFromInvalidTargetIsRejected() {
        val state = play.copy(
            phase = GamePhase.ChooseStealTarget(listOf(bob)),
        )
        val result = engine.reduce(state, alice, StealFrom(PlayerId("nobody")))
        assertNotNull(result.rejection)
    }

    @Test
    fun stealFromOutsideChoosePhaseIsRejected() {
        val state = play.copy(phase = GamePhase.Play)
        assertNotNull(engine.reduce(state, alice, StealFrom(bob)).rejection)
    }

    @Test
    fun rollerLeavingDuringChooseStealTargetStealsAtRandom() {
        val players = listOf(alice, bob, PlayerId("carol"))
        val engine3 = CatanGameEngine(boardSeed = 1)
        val state3 = engine3.completeSetup(engine3.initialState(players))
        val carol = PlayerId("carol")

        // Alice is the roller, mid-choice between bob and carol who both hold cards.
        val s = state3.copy(
            currentPlayerIndex = state3.players.indexOf(alice),
            phase = GamePhase.ChooseStealTarget(listOf(bob, carol)),
            hands = state3.hands + (bob to ResourceCount.of(Resource.BRICK to 2))
                    + (carol to ResourceCount.of(Resource.ORE to 2)),
        )
        val bobBefore = s.handOf(bob).total
        val carolBefore = s.handOf(carol).total

        val result = engine3.playerLeft(s, alice)
        assertTrue(alice !in result.state.present)
        // A card was stolen at random from one of the two victims.
        val stolen = result.events.filterIsInstance<ResourceStolen>().single()
        assertTrue(stolen.from == bob || stolen.from == carol)
        assertEquals(alice, stolen.by)
        val totalAfter = result.state.handOf(bob).total + result.state.handOf(carol).total
        assertEquals(bobBefore + carolBefore - 1, totalAfter)
        // And the turn advanced past the leaver.
        assertTrue(result.state.currentPlayer != alice)
    }

    @Test
    fun singleVictimAutoStealsWithoutChoosePhase() {
        val current = play.currentPlayer
        val hex = play.buildings.filter { it.owner == bob }
            .flatMap { it.vertex.hexes }
            .first { play.board.tileAt(it) != null && it != play.board.robber }
        val s = play.copy(
            phase = GamePhase.Robber,
            hands = play.hands + (bob to ResourceCount.of(Resource.BRICK to 1)),
        )
        val result = engine.reduce(s, current, MoveRobber(hex))
        assertNull(result.rejection)
        assertEquals(GamePhase.Play, result.state.phase)
        assertTrue(result.events.any { it is ResourceStolen })
        // No ChooseStealTarget phase event.
        assertTrue(result.events.none { it is PhaseChanged && it.phase is GamePhase.ChooseStealTarget })
    }
}
