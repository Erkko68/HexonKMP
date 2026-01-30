package eric.bitria.hexon.game.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HexCoordAsStringSerializer : KSerializer<HexCoord> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HexCoord", STRING)

    override fun serialize(encoder: Encoder, value: HexCoord) {
        // Encodes as "q,r"
        encoder.encodeString("${value.q},${value.r}")
    }

    override fun deserialize(decoder: Decoder): HexCoord {
        // Decodes from "q,r"
        val parts = decoder.decodeString().split(",")
        return HexCoord(parts[0].toInt(), parts[1].toInt())
    }
}

// 2. Annotate the class to use this serializer
@Serializable(with = HexCoordAsStringSerializer::class)
data class HexCoord(val q: Int, val r: Int) : Comparable<HexCoord> {
    // (0,0)-(1,0) is same as (1,0)-(0,0)
    override fun compareTo(other: HexCoord): Int {
        if (this.q != other.q) return this.q - other.q
        return this.r - other.r
    }

    override fun toString() = "$q,$r"

    companion object {

        fun getHexId(h: HexCoord): String = "${h.q},${h.r}"

        fun getEdgeId(h1: HexCoord, h2: HexCoord): String {
            val sorted = listOf(h1, h2).sorted()
            return "${sorted[0]}|${sorted[1]}" // e.g. "0,0|1,-1"
        }

        fun getVertexId(h1: HexCoord, h2: HexCoord, h3: HexCoord): String {
            val sorted = listOf(h1, h2, h3).sorted()
            return "${sorted[0]}|${sorted[1]}|${sorted[2]}" // e.g. "0,0|0,1|1,0"
        }
    }
}