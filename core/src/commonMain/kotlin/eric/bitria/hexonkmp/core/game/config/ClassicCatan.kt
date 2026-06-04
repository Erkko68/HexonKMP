package eric.bitria.hexonkmp.core.game.config

import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.core.game.model.Port
import eric.bitria.hexonkmp.core.game.model.board.Axial
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.core.game.model.board.Terrain
import eric.bitria.hexonkmp.core.game.model.board.cornerVertex
import eric.bitria.hexonkmp.core.game.model.board.hexagonalLayout

// The 19-hex classic board layout, shared by the tiles and the port placement.
private val classicLayout: List<Axial> = hexagonalLayout(radius = 2)

// The standard 3–4 player Catan game, expressed entirely as data. This is the
// reference example of a "game mode": nothing here is special-cased in the
// engine — a variant would be another value just like this one.
val ClassicCatan: ScenarioConfig = ScenarioConfig(
    name = "Classic Catan",
    hexLayout = classicLayout, // 19 hexes
    terrainBag = buildList {
        repeat(4) { add(Terrain.FOREST) }    // lumber
        repeat(4) { add(Terrain.PASTURE) }   // wool
        repeat(4) { add(Terrain.FIELDS) }    // grain
        repeat(3) { add(Terrain.HILLS) }     // brick
        repeat(3) { add(Terrain.MOUNTAINS) } // ore
        add(Terrain.DESERT)
    },
    // 18 chits for the 18 non-desert tiles (no 7 — that's the robber roll).
    numberTokens = listOf(2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12),
    rules = RuleConfig(
        minPlayers = 2, // dev-friendly; classic is 3
        maxPlayers = 4,
        autoStartDelaySeconds = 10,
        victoryPointsToWin = 10,
        buildCosts = mapOf(
            Buildable.ROAD to mapOf(Resource.BRICK to 1, Resource.LUMBER to 1),
            Buildable.SETTLEMENT to mapOf(
                Resource.BRICK to 1, Resource.LUMBER to 1,
                Resource.WOOL to 1, Resource.GRAIN to 1,
            ),
            Buildable.CITY to mapOf(Resource.ORE to 3, Resource.GRAIN to 2),
            Buildable.DEV_CARD to mapOf(Resource.ORE to 1, Resource.WOOL to 1, Resource.GRAIN to 1),
        ),
        pieceLimits = mapOf(
            Buildable.ROAD to 15,
            Buildable.SETTLEMENT to 5,
            Buildable.CITY to 4,
        ),
        // Classic 25-card deck: 14 knights, 5 victory points, 2 each progress card.
        devCardDeck = mapOf(
            DevCard.KNIGHT to 14,
            DevCard.VICTORY_POINT to 5,
            DevCard.ROAD_BUILDING to 2,
            DevCard.YEAR_OF_PLENTY to 2,
            DevCard.MONOPOLY to 2,
        ),
    ),
    ports = classicPorts(classicLayout),
)

// The classic harbor multiset: four generic 3:1 ports plus one 2:1 port per
// resource. PROVISIONAL placement — each is dropped on a single coastline vertex,
// spread deterministically around the perimeter. The authentic two-vertex harbor
// positions land with the board's harbor rendering; the engine treats whatever
// vertices appear here as the ports, so only this data changes.
private fun classicPorts(layout: List<Axial>): List<Port> {
    val board = layout.toSet()
    // A coastline vertex: a corner touching at least one board hex and open sea.
    val perimeter = buildSet {
        for (hex in layout) for (k in 0..5) {
            val v = cornerVertex(hex, k)
            if (v.hexes.any { it in board } && v.hexes.any { it !in board }) add(v)
        }
    }.sortedWith(
        compareBy(
            { it.hexes[0].q }, { it.hexes[0].r },
            { it.hexes[1].q }, { it.hexes[1].r },
            { it.hexes[2].q }, { it.hexes[2].r },
        ),
    )
    if (perimeter.isEmpty()) return emptyList()
    val kinds: List<Pair<Resource?, Int>> = listOf(
        null to 3, null to 3, null to 3, null to 3,
        Resource.LUMBER to 2, Resource.BRICK to 2, Resource.WOOL to 2,
        Resource.GRAIN to 2, Resource.ORE to 2,
    )
    val step = (perimeter.size / kinds.size).coerceAtLeast(1)
    return kinds.mapIndexed { i, (resource, ratio) ->
        Port(perimeter[(i * step) % perimeter.size], resource, ratio)
    }
}
