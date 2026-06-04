package eric.bitria.hexonkmp.ui.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.ui.components.cards.ResourceCard
import eric.bitria.hexonkmp.ui.theme.Spacing

// Forced "discard half" sheet shown after a 7 when you're over the hand limit.
// It can't be dismissed — you must discard exactly [required] cards. Mirrors the
// trade propose form: tap-to-cycle resource tokens, Clear + Submit below.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscardSheet(
    required: Int,
    hand: ResourceCount,
    selected: ResourceCount,
    onCycle: (Resource) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
) {
    // Non-dismissible: block drag/scrim from hiding the sheet.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden },
    )
    ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Too many cards!", style = MaterialTheme.typography.titleSmall)
            Text(
                "Discard $required  (${selected.total} / $required selected)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            ) {
                Resource.entries.forEach { r ->
                    ResourceCard(
                        resource = r,
                        count = selected[r].takeIf { it > 0 },
                        selected = selected[r] > 0,
                        enabled = hand[r] > 0,
                        onClick = { onCycle(r) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = onClear, enabled = !selected.isEmpty) { Text("Clear") }
                Button(onClick = onSubmit, enabled = selected.total == required) { Text("Discard") }
            }
        }
    }
}
