package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.EndTurn
import eric.bitria.hexonkmp.core.game.action.MoveRobber
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.event.ResourceStolen
import eric.bitria.hexonkmp.core.game.model.Building
import eric.bitria.hexonkmp.core.game.model.GamePhase
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.cornerVertex
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The robber: a 7 sends the current player into the Robber phase; they relocate
// the robber, auto-stealing a random card from a random adjacent opponent, and
// the robbed tile stops producing.
class RobberTest {

    private val engine = CatanGameEngine(boardSeed = 1)
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))

    // A real tile that bob has a building on, other than the current robber tile.
    private fun bobTileHex() = play.buildings.filter { it.owner == bob }
        .flatMap { it.vertex.hexes }
        .first { play.board.tileAt(it) != null && it != play.board.robber }

    @Test
    fun moveRobberStealsACardAndReturnsToPlay() {
        val current = play.currentPlayer // alice
        val hex = bobTileHex()
        val s = play.copy(
            phase = GamePhase.Robber,
            hands = play.hands + (bob to ResourceCount.of(eric.bitria.hexonkmp.core.game.model.board.Resource.BRICK to 3)),
        )
        val before = s.handOf(current).total
        val result = engine.reduce(s, current, MoveRobber(hex))
        assertNull(result.rejection)
        assertEquals(hex, result.state.board.robber)
        assertEquals(GamePhase.Play, result.state.phase)
        val stolen = result.events.filterIsInstance<ResourceStolen>().single()
        assertEquals(current, stolen.by)
        assertTrue(stolen.from != current)
        assertEquals(before + 1, result.state.handOf(current).total)
    }

    @Test
    fun movingToTheSameTileIsRejected() {
        val s = play.copy(phase = GamePhase.Robber)
        val here = s.board.robber!!
        assertNotNull(engine.reduce(s, s.currentPlayer, MoveRobber(here)).rejection)
    }

    @Test
    fun movingOutsideTheRobberPhaseIsRejected() {
        val s = play.copy(phase = GamePhase.Play)
        assertNotNull(engine.reduce(s, s.currentPlayer, MoveRobber(bobTileHex())).rejection)
    }

    @Test
    fun nothingStolenWhenAdjacentPlayersAreEmptyHanded() {
        val current = play.currentPlayer
        val s = play.copy(
            phase = GamePhase.Robber,
            hands = play.players.associateWith { ResourceCount() },
        )
        val result = engine.reduce(s, current, MoveRobber(bobTileHex()))
        assertNull(result.rejection)
        assertTrue(result.events.none { it is ResourceStolen })
    }

    @Test
    fun robberBlocksProductionOnItsTile() {
        val tile = play.board.tiles.first { it.token != null && it.terrain.resource != null }
        val res = tile.terrain.resource!!
        val other = play.board.tiles.first { it.hex != tile.hex }.hex
        // Seed whose next two dice sum to this tile's token (never a 7 here).
        val seed = generateSequence(0L) { it + 1 }.first { s ->
            val r = Random(s); r.nextInt(1, 7) + r.nextInt(1, 7) == tile.token
        }
        val base = play.copy(
            phase = GamePhase.Play,
            buildings = listOf(Building(alice, cornerVertex(tile.hex, 0), Building.Kind.SETTLEMENT)),
            hands = mapOf(alice to ResourceCount(), bob to ResourceCount()),
            rngSeed = seed,
            currentPlayerIndex = play.players.indexOf(alice),
        )
        // Same forced roll: blocked when the robber sits on the tile, produces otherwise.
        val blocked = engine.reduce(base.copy(board = base.board.copy(robber = tile.hex)), alice, EndTurn)
        val open = engine.reduce(base.copy(board = base.board.copy(robber = other)), alice, EndTurn)
        assertEquals(tile.token, blocked.state.lastRoll) // sanity: the forced roll hit the token
        assertEquals(0, blocked.state.handOf(alice)[res])
        assertTrue(open.state.handOf(alice)[res] >= 1)
    }
}
