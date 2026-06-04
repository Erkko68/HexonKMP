package eric.bitria.hexonkmp.core.game

import eric.bitria.hexonkmp.core.game.action.BankTrade
import eric.bitria.hexonkmp.core.game.model.Port
import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.event.BankTraded
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.core.game.model.board.endpoints
import eric.bitria.hexonkmp.core.game.model.board.incidentEdges
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BankTradeTest {

    private val engine = CatanGameEngine(boardSeed = 1)
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val play = engine.completeSetup(engine.initialState(listOf(alice, bob)))
    private val ratio = play.config.rules.bankTradeRatio

    // A coastline edge with one endpoint on the current player's building (a harbor
    // there is reachable), and an edge reaching none of their buildings.
    private val ownedPortEdge: Edge =
        play.buildings.first { it.owner == play.currentPlayer }.vertex.incidentEdges().first()
    private val unownedPortEdge: Edge = run {
        val mine = play.buildings.filter { it.owner == play.currentPlayer }.map { it.vertex }.toSet()
        play.board.edges().first { e -> e.endpoints().none { it in mine } }
    }

    // Puts [ports] on the board (harbors are board state, placed by BoardGenerator).
    private fun GameState.withPorts(vararg ports: Port) = copy(board = board.copy(ports = ports.toList()))

    // Gives the current player [hand] and trades it for [receive] at the bank.
    private fun trade(give: ResourceCount, receive: ResourceCount, state: GameState = play) =
        engine.reduce(
            state.copy(hands = state.hands + (state.currentPlayer to give)),
            state.currentPlayer,
            BankTrade(give, receive),
        )

    @Test
    fun tradesAtTheBankRatio() {
        val result = trade(ResourceCount.of(Resource.ORE to ratio), ResourceCount.of(Resource.BRICK to 1))
        assertNull(result.rejection)
        val hand = result.state.handOf(play.currentPlayer)
        assertEquals(0, hand[Resource.ORE])     // gave away `ratio` ore
        assertEquals(1, hand[Resource.BRICK])   // received 1 brick
        assertEquals(
            BankTraded(play.currentPlayer, ResourceCount.of(Resource.ORE to ratio), ResourceCount.of(Resource.BRICK to 1)),
            result.events.single(),
        )
    }

    @Test
    fun multipleOutputsApplyAtomically() {
        // 2*ratio ore -> 1 brick + 1 grain in one trade.
        val result = trade(
            ResourceCount.of(Resource.ORE to ratio * 2),
            ResourceCount.of(Resource.BRICK to 1, Resource.GRAIN to 1),
        )
        assertNull(result.rejection)
        val hand = result.state.handOf(play.currentPlayer)
        assertEquals(0, hand[Resource.ORE])
        assertEquals(1, hand[Resource.BRICK])
        assertEquals(1, hand[Resource.GRAIN])
    }

    @Test
    fun mixedGiveResourcesFundMultipleOutputs() {
        // ratio ore + ratio wool -> 2 bricks (each give funds one output).
        val result = trade(
            ResourceCount.of(Resource.ORE to ratio, Resource.WOOL to ratio),
            ResourceCount.of(Resource.BRICK to 2),
        )
        assertNull(result.rejection)
        assertEquals(2, result.state.handOf(play.currentPlayer)[Resource.BRICK])
    }

    @Test
    fun unbalancedTradeIsRejected() {
        // ratio ore only funds one output, but two are requested.
        val result = trade(ResourceCount.of(Resource.ORE to ratio), ResourceCount.of(Resource.BRICK to 2))
        assertNotNull(result.rejection)
    }

    @Test
    fun nonMultipleGiveIsRejected() {
        // ratio+1 ore is not an exact multiple of the ratio.
        val result = trade(ResourceCount.of(Resource.ORE to ratio + 1), ResourceCount.of(Resource.BRICK to 1))
        assertNotNull(result.rejection)
    }

    @Test
    fun tradeWithoutEnoughResourcesIsRejected() {
        val current = play.currentPlayer
        val s = play.copy(hands = play.hands + (current to ResourceCount.of(Resource.ORE to ratio - 1)))
        val result = engine.reduce(s, current, BankTrade(ResourceCount.of(Resource.ORE to ratio), ResourceCount.of(Resource.BRICK to 1)))
        assertNotNull(result.rejection)
    }

    @Test
    fun tradeForSameResourceIsRejected() {
        val result = trade(ResourceCount.of(Resource.ORE to ratio), ResourceCount.of(Resource.ORE to 1))
        assertNotNull(result.rejection)
    }

    @Test
    fun onlyCurrentPlayerCanTrade() {
        val other = play.players.first { it != play.currentPlayer }
        val s = play.copy(hands = play.hands + (other to ResourceCount.of(Resource.ORE to ratio)))
        val result = engine.reduce(s, other, BankTrade(ResourceCount.of(Resource.ORE to ratio), ResourceCount.of(Resource.BRICK to 1)))
        assertNotNull(result.rejection)
    }

    // --- Ports ---

    @Test
    fun genericPortLowersEveryRatio() {
        val withPort = play.withPorts(Port(ownedPortEdge, resource = null, ratio = 3))
        val rates = engine.bankRates(withPort, withPort.currentPlayer)
        assertEquals(Resource.entries.associateWith { 3 }, rates)
        // 3 ore -> 1 brick is now valid (would be 4:1 without the port).
        assertNull(trade(ResourceCount.of(Resource.ORE to 3), ResourceCount.of(Resource.BRICK to 1), withPort).rejection)
    }

    @Test
    fun specificPortLowersOnlyItsResource() {
        val withPort = play.withPorts(Port(ownedPortEdge, resource = Resource.ORE, ratio = 2))
        val rates = engine.bankRates(withPort, withPort.currentPlayer)
        assertEquals(2, rates[Resource.ORE])
        assertEquals(ratio, rates[Resource.WOOL])
        // 2 ore -> 1 brick valid (port); 2 wool -> 1 brick invalid (still base ratio).
        assertNull(trade(ResourceCount.of(Resource.ORE to 2), ResourceCount.of(Resource.BRICK to 1), withPort).rejection)
        assertNotNull(trade(ResourceCount.of(Resource.WOOL to 2), ResourceCount.of(Resource.BRICK to 1), withPort).rejection)
    }

    @Test
    fun portReachesEitherEndpointOfItsEdge() {
        // The port sits on an edge; a building on either endpoint grants it.
        val withPort = play.withPorts(Port(ownedPortEdge, resource = null, ratio = 3))
        val ownsAnEndpoint = ownedPortEdge.endpoints().any { v ->
            play.buildings.any { it.owner == play.currentPlayer && it.vertex == v }
        }
        assertEquals(true, ownsAnEndpoint)
        assertEquals(3, engine.bankRates(withPort, withPort.currentPlayer)[Resource.ORE])
    }

    @Test
    fun portOnUnreachedEdgeDoesNotApply() {
        val withPort = play.withPorts(Port(unownedPortEdge, resource = null, ratio = 2))
        assertEquals(ratio, engine.bankRates(withPort, withPort.currentPlayer)[Resource.ORE])
    }
}
