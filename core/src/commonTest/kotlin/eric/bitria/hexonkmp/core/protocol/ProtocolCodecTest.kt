package eric.bitria.hexonkmp.core.protocol

import eric.bitria.hexonkmp.core.game.engine.CatanGameEngine
import eric.bitria.hexonkmp.core.game.event.DiceRolled
import eric.bitria.hexonkmp.core.game.model.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// Verifies the generic ServerEvent<S, E> serializes round-trip through CatanCodec
// — the part of the generic-protocol refactor most likely to bite (kotlinx
// polymorphic serialization of a generic sealed hierarchy with Nothing args).
class ProtocolCodecTest {

    private val alice = PlayerId("alice")

    @Test
    fun gameStartedSnapshotRoundTrips() {
        val state = CatanGameEngine(boardSeed = 1).initialState(listOf(alice, PlayerId("bob")))
        val decoded = CatanCodec.decodeServerEvent(CatanCodec.encodeServerEvent(GameStarted(state)))
        assertIs<GameStarted<*>>(decoded)
        assertEquals(state, decoded.state)
    }

    @Test
    fun gameUpdateEventRoundTrips() {
        val event = DiceRolled(3, 4, 7)
        val decoded = CatanCodec.decodeServerEvent(CatanCodec.encodeServerEvent(GameUpdate(event)))
        assertIs<GameUpdate<*>>(decoded)
        assertEquals(event, decoded.event)
    }

    @Test
    fun agnosticLifecycleMessageRoundTrips() {
        val roster = LobbyRoster(
            members = listOf(LobbyMember("alice", "Alice"), LobbyMember("bob", "Bob")),
            hostId = "alice",
            minPlayers = 2,
            maxPlayers = 4,
            countdownSeconds = 30,
        )
        val decoded = CatanCodec.decodeServerEvent(CatanCodec.encodeServerEvent(roster))
        assertEquals(roster, decoded)
    }
}
