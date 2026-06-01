package eric.bitria.hexonkmp.core.game.model

import eric.bitria.hexonkmp.core.game.model.board.Resource
import kotlinx.serialization.Serializable

// A bag of resources — a player's hand, or an amount produced/spent. Backed by a
// map so zero-quantity entries are simply absent. Immutable; arithmetic returns
// a new value. Pure data, no rules.
@Serializable
data class ResourceCount(val amounts: Map<Resource, Int> = emptyMap()) {

    operator fun get(resource: Resource): Int = amounts[resource] ?: 0

    val total: Int get() = amounts.values.sum()

    val isEmpty: Boolean get() = amounts.values.all { it == 0 }

    operator fun plus(other: ResourceCount): ResourceCount =
        combine(other) { a, b -> a + b }

    operator fun minus(other: ResourceCount): ResourceCount =
        combine(other) { a, b -> a - b }

    // True if this hand covers every resource in `cost` (for affording builds).
    fun covers(cost: ResourceCount): Boolean =
        cost.amounts.all { (res, qty) -> this[res] >= qty }

    private fun combine(other: ResourceCount, op: (Int, Int) -> Int): ResourceCount {
        val keys = amounts.keys + other.amounts.keys
        val merged = keys.associateWith { op(this[it], other[it]) }
            .filterValues { it != 0 }
        return ResourceCount(merged)
    }

    companion object {
        fun of(vararg pairs: Pair<Resource, Int>): ResourceCount =
            ResourceCount(pairs.toMap().filterValues { it != 0 })
    }
}
