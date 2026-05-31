package eric.bitria.hexonkmp.core.protocol

import eric.bitria.hexonkmp.core.AppJson
import eric.bitria.hexonkmp.core.game.action.GameAction

// Single home for (de)serializing the wire protocol. Lives in core, which has
// the serialization plugin + runtime, and uses explicit polymorphic serializers
// via StringFormat's member functions — so callers (client, server) need no
// kotlinx.serialization imports and there's one place that defines the format.
object Wire {
    fun encode(event: ServerEvent): String =
        AppJson.encodeToString(ServerEvent.serializer(), event)

    fun decodeEvent(text: String): ServerEvent =
        AppJson.decodeFromString(ServerEvent.serializer(), text)

    fun encode(action: GameAction): String =
        AppJson.encodeToString(GameAction.serializer(), action)

    fun decodeAction(text: String): GameAction =
        AppJson.decodeFromString(GameAction.serializer(), text)
}
