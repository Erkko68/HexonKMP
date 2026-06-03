package eric.bitria.hexonkmp.core.game.model

import kotlinx.serialization.Serializable

// The development-card types. Cards are drawn from a shuffled deck whose
// composition is data (RuleConfig.devCardDeck). Most are played for an effect;
// VICTORY_POINT is never played — it silently counts toward the win.
//
// A player's held dev cards are HIDDEN information: opponents only ever learn how
// MANY a player holds, never which ones (until a card is played, which reveals
// it). That secrecy is enforced at the transport seam by GameState.redactedFor —
// see that function. There is deliberately no "face-down" sentinel in this enum:
// the domain stays clean, and redaction exposes counts via a separate field.
@Serializable
enum class DevCard { KNIGHT, VICTORY_POINT, ROAD_BUILDING, YEAR_OF_PLENTY, MONOPOLY }
