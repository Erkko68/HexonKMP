package eric.bitria.hexonkmp.core.game.engine

import eric.bitria.hexonkmp.core.game.event.GameEvent
import eric.bitria.hexonkmp.core.game.model.GameState

// Outcome of reducing one action. On success `state` is the new authoritative
// state and `events` describe the change; on rejection `state` is unchanged and
// `rejection` explains why (shown only to the acting player). Not serialized —
// it never crosses the wire, the server turns `events` into transport messages.
data class GameResult(
    val state: GameState,
    val events: List<GameEvent> = emptyList(),
    val rejection: String? = null,
)
