package eric.bitria.hexonkmp.core.game.config

import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.core.game.model.board.Terrain
import eric.bitria.hexonkmp.core.game.model.board.hexagonalLayout

// The standard 3–4 player Catan game, expressed entirely as data. This is the
// reference example of a "game mode": nothing here is special-cased in the
// engine — a variant would be another value just like this one.
val ClassicCatan: ScenarioConfig = ScenarioConfig(
    name = "Classic Catan",
    hexLayout = hexagonalLayout(radius = 2), // 19 hexes
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
    ),
)
