package eric.bitria.hexonkmp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.action.BankSwap
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.ui.theme.Spacing

// The trade sheet — a mobile-friendly bottom sheet with two tabs: Bank (active)
// and Players (placeholder for upcoming player-to-player trades). Always
// openable; the bank tab simply offers nothing tradable when the hand is short.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeSheet(
    ratio: Int,
    hand: ResourceCount,
    onBankTrade: (List<BankSwap>) -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    // Skip the half-expanded state so the whole trade form (down to the Confirm
    // button) shows in one step; the sheet itself wraps its content height.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Bank") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Players") })
            }
            Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
                when (tab) {
                    0 -> BankTab(ratio, hand, onBankTrade)
                    else -> PlayersTab()
                }
            }
        }
    }
}

@Composable
private fun BankTab(
    ratio: Int,
    hand: ResourceCount,
    onConfirm: (List<BankSwap>) -> Unit,
) {
    val swaps = remember { mutableStateListOf<BankSwap>() }
    var give by remember { mutableStateOf<Resource?>(null) }
    var get by remember { mutableStateOf<Resource?>(null) }

    // Resources still affordable after the gives already queued.
    val queued = remember(swaps.size) {
        var rc = ResourceCount()
        swaps.forEach { rc += ResourceCount.of(it.give to ratio) }
        rc
    }
    fun canGive(r: Resource) = hand[r] - queued[r] >= ratio

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Trade $ratio : 1 with the bank", style = MaterialTheme.typography.titleSmall)

        // Queued swaps as token -> token rows.
        swaps.forEachIndexed { i, swap ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            ) {
                ResourceToken(swap.give, count = ratio, size = 44)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "for")
                ResourceToken(swap.get, count = 1, size = 44)
                IconButton(onClick = { swaps.removeAt(i) }) {
                    Icon(Icons.Filled.Close, contentDescription = "remove")
                }
            }
        }

        // Give picker: every resource, dimmed/disabled when unaffordable.
        Text("Give", style = MaterialTheme.typography.labelMedium)
        TokenRow {
            Resource.entries.forEach { r ->
                ResourceToken(
                    resource = r,
                    count = hand[r],
                    selected = give == r,
                    enabled = canGive(r),
                    onClick = { give = r; if (get == r) get = null },
                )
            }
        }

        // Get picker: any resource other than the chosen give.
        Text("Receive", style = MaterialTheme.typography.labelMedium)
        TokenRow {
            Resource.entries.forEach { r ->
                ResourceToken(
                    resource = r,
                    selected = get == r,
                    enabled = r != give,
                    onClick = { get = r },
                )
            }
        }

        OutlinedButton(
            onClick = {
                val g = give; val r = get
                if (g != null && r != null) { swaps.add(BankSwap(g, r)); give = null; get = null }
            },
            enabled = give != null && get != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Add swap") }

        Button(
            onClick = { onConfirm(swaps.toList()) },
            enabled = swaps.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (swaps.isEmpty()) "Confirm trade" else "Confirm trade (${swaps.size})") }
    }
}

@Composable
private fun PlayersTab() {
    Box(Modifier.fillMaxWidth().padding(vertical = Spacing.xxl), contentAlignment = Alignment.Center) {
        Text(
            "Player trading coming soon",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TokenRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) { content() }
}
