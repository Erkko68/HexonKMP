package eric.bitria.hexonkmp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eric.bitria.hexonkmp.core.game.action.BankSwap
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.ui.theme.Spacing

// Bottom sheet for bank trades — non-obstructive on mobile, swipe to dismiss.
// The player builds up a list of swaps (each: `ratio` of one resource -> 1 of
// another); only resources they can still afford (after the swaps queued so far)
// are offered as the "give". Confirm sends all queued swaps as one atomic trade.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankTradeSheet(
    ratio: Int,
    hand: ResourceCount,
    onConfirm: (List<BankSwap>) -> Unit,
    onDismiss: () -> Unit,
) {
    val swaps = remember { mutableStateListOf<BankSwap>() }
    var give by remember { mutableStateOf<Resource?>(null) }
    var get by remember { mutableStateOf<Resource?>(null) }

    // Resource is giveable if the hand covers the already-queued gives + one more
    // `ratio` of it.
    val queuedGive = remember(swaps.size) {
        var rc = ResourceCount()
        swaps.forEach { rc += ResourceCount.of(it.give to ratio) }
        rc
    }
    fun canGive(r: Resource) = hand[r] - queuedGive[r] >= ratio
    val giveable = Resource.entries.filter { canGive(it) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md).padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Bank trade ($ratio : 1)", style = MaterialTheme.typography.titleMedium)

            // Queued swaps.
            if (swaps.isNotEmpty()) {
                swaps.forEachIndexed { i, swap ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text("$ratio× ${ResourceVisuals.label(swap.give)}", style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.Filled.ArrowForward, contentDescription = "for", modifier = Modifier.padding(horizontal = Spacing.xs))
                        Text("1× ${ResourceVisuals.label(swap.get)}", style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { swaps.removeAt(i) }) {
                            Icon(Icons.Filled.Close, contentDescription = "remove")
                        }
                    }
                }
            }

            // New swap builder.
            Text("Give $ratio×", style = MaterialTheme.typography.labelMedium)
            ResourceChips(giveable, give) { give = it; if (get == it) get = null }
            Text("Receive 1×", style = MaterialTheme.typography.labelMedium)
            ResourceChips(Resource.entries.filter { it != give }, get) { get = it }

            OutlinedButton(
                onClick = {
                    val g = give; val r = get
                    if (g != null && r != null) {
                        swaps.add(BankSwap(g, r)); give = null; get = null
                    }
                },
                enabled = give != null && get != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add swap") }

            Button(
                onClick = { onConfirm(swaps.toList()) },
                enabled = swaps.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (swaps.isEmpty()) "Trade" else "Trade (${swaps.size})") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourceChips(options: List<Resource>, selected: Resource?, onSelect: (Resource) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        options.forEach { res ->
            FilterChip(
                selected = selected == res,
                onClick = { onSelect(res) },
                label = { Text(ResourceVisuals.label(res)) },
            )
        }
    }
}
