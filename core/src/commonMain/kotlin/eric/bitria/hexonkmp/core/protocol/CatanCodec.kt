package eric.bitria.hexonkmp.core.protocol

import eric.bitria.hexonkmp.core.AppJson
import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.game.event.GameEvent
import eric.bitria.hexonkmp.core.game.model.GameState

// The server→client envelope bound to Catan's state/event types. Lets the client
// refer to the generic ServerEvent without spelling out the type arguments.
typealias CatanServerEvent = ServerEvent<GameState, GameEvent>

// The Catan game's GameCodec: the single home for (de)serializing Catan's wire
// payloads. It builds the generic ServerEvent serializer from Catan's concrete
// state/event serializers, so callers (server, client) need no kotlinx imports.
object CatanCodec : GameCodec<GameState, GameAction, GameEvent> {

    private val serverEvent = ServerEvent.serializer(GameState.serializer(), GameEvent.serializer())

    override fun encodeServerEvent(event: ServerEvent<GameState, GameEvent>): String =
        AppJson.encodeToString(serverEvent, event)

    override fun decodeServerEvent(text: String): ServerEvent<GameState, GameEvent> =
        AppJson.decodeFromString(serverEvent, text)

    override fun encodeAction(action: GameAction): String =
        AppJson.encodeToString(GameAction.serializer(), action)

    override fun decodeAction(text: String): GameAction =
        AppJson.decodeFromString(GameAction.serializer(), text)
}
