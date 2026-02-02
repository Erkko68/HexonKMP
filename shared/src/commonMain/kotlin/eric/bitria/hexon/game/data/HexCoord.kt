package eric.bitria.hexon.game.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HexCoordKeySerializer : KSerializer<HexCoord> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("HexCoord", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: HexCoord) {
        encoder.encodeString(value.toString()) // "q,r"
    }

    override fun deserialize(decoder: Decoder): HexCoord {
        return HexCoord.fromHexId(decoder.decodeString())
    }
}

// 2. Annotate the class to use this serializer
@Serializable(with = HexCoordKeySerializer::class)
data class HexCoord(val q: Int, val r: Int) : Comparable<HexCoord> {
    // (0,0)-(1,0) is same as (1,0)-(0,0)
    override fun compareTo(other: HexCoord): Int {
        if (this.q != other.q) return this.q - other.q
        return this.r - other.r
    }

    override fun toString() = "$q,$r"

    companion object {

        fun getHexId(h: HexCoord): String = h.toString()

        fun getEdgeId(h1: HexCoord, h2: HexCoord): EdgeId {
            val sorted = listOf(h1, h2).sorted()
            return "${sorted[0]}|${sorted[1]}" // e.g. "0,0|1,-1"
        }

        fun getVertexId(h1: HexCoord, h2: HexCoord, h3: HexCoord): VertexId {
            val sorted = listOf(h1, h2, h3).sorted()
            return "${sorted[0]}|${sorted[1]}|${sorted[2]}" // e.g. "0,0|0,1|1,0"
        }

        fun fromHexId(id: String): HexCoord =
            id.split(",")
                .let {
                    require(it.size == 2) { "Invalid HexCoord id: $id" }
                    HexCoord(it[0].toInt(), it[1].toInt())
                }

        fun fromEdgeId(id: EdgeId): Pair<HexCoord, HexCoord> =
            id.split("|")
                .map(::fromHexId)
                .also { require(it.size == 2) { "Invalid Edge id: $id" } }
                .let { it[0] to it[1] }

        fun fromVertexId(id: VertexId): Triple<HexCoord, HexCoord, HexCoord> =
            id.split("|")
                .map(::fromHexId)
                .also { require(it.size == 3) { "Invalid Vertex id: $id" } }
                .let { Triple(it[0], it[1], it[2]) }

    }
}