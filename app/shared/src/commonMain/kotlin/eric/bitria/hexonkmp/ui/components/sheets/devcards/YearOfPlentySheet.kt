package eric.bitria.hexonkmp.ui.components.sheets.devcards

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.ui.components.cards.ResourceCard
import eric.bitria.hexonkmp.ui.theme.Spacing

// Year of Plenty sheet: tap-to-pick any 2 resources from the bank (any mix,
// including two of the same). Submit sends the action; Cancel dismisses.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearOfPlentySheet(onSubmit: (ResourceCount) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(ResourceCount()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Year of Plenty", style = MaterialTheme.typography.titleSmall)
            Text(
                "Take 2 resources from the bank  (${selected.total} / 2)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            ) {
                Resource.entries.forEach { r ->
                    val count = selected[r]
                    ResourceCard(
                        resource = r,
                        count = count.takeIf { it > 0 },
                        selected = count > 0,
                        enabled = selected.total < 2 || count > 0,
                        size = 40.dp,
                        onClick = {
                            selected = when {
                                count > 0 && selected.total == 2 ->
                                    ResourceCount((selected.amounts + (r to count - 1)).filterValues { it != 0 })
                                selected.total < 2 ->
                                    ResourceCount(selected.amounts + (r to count + 1))
                                else -> selected
                            }
                        },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                OutlinedButton(
                    onClick = { selected = ResourceCount() },
                    enabled = !selected.isEmpty,
                ) { Text("Clear") }
                Button(
                    onClick = { onDismiss(); onSubmit(selected) },
                    enabled = selected.total == 2,
                ) { Text("Take") }
            }
        }
    }
}
