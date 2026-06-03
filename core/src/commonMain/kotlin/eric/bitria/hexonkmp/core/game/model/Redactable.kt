package eric.bitria.hexonkmp.core.game.model

// The capability the transport layer relies on to enforce hidden information:
// produce the version of this value that [viewer] is allowed to see. The pure
// engine always works on full truth; the server calls redactedFor per recipient
// before shipping a snapshot or event. Implemented by GameState and GameEvent.
//
// This is what lets the transport (GameSession) depend on a ROLE — "something I
// can redact for a viewer" — rather than on Catan's concrete types.
interface Redactable<out T> {
    fun redactedFor(viewer: PlayerId): T
}
