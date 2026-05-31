package eric.bitria.hexonkmp.core.game.model

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

// Type-safe player identifier. Serializes transparently as its String value.
@Serializable
@JvmInline
value class PlayerId(val value: String)
