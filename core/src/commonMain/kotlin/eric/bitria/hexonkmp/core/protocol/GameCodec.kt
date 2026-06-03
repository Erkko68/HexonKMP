package eric.bitria.hexonkmp.core.protocol

// (De)serialization seam for one game, generic over its state [S], action [A],
// and event [E]. The transport (server GameSession, client GameClient) speaks
// only this interface, so it needs no kotlinx.serialization imports and stays
// game-agnostic: a new game provides its own GameCodec and the transport is
// unchanged. The server-bound envelope ServerEvent<S, E> carries the game payload
// (snapshot / event); actions travel client → server as their own messages.
interface GameCodec<S, A, E> {
    fun encodeServerEvent(event: ServerEvent<S, E>): String
    fun decodeServerEvent(text: String): ServerEvent<S, E>

    fun encodeAction(action: A): String
    fun decodeAction(text: String): A
}
