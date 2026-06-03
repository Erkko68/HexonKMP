package eric.bitria.hexonkmp.core.game.engine

// Outcome of reducing one action, generic over a game's state [S] and event [E]
// types. On success `state` is the new authoritative state and `events` describe
// the change; on rejection `state` is unchanged and `rejection` explains why
// (shown only to the acting player). Not serialized — it never crosses the wire;
// the server turns `events` into transport messages.
data class GameResult<out S, out E>(
    val state: S,
    val events: List<E> = emptyList(),
    val rejection: String? = null,
)
