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
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.ui.components.cards.ResourceCard
import eric.bitria.hexonkmp.ui.theme.Spacing

// Monopoly sheet: tap a resource type to select it, then confirm to take all
// of that resource from every other player. Cancel dismisses without action.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonopolySheet(onSubmit: (Resource) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf<Resource?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Monopoly", style = MaterialTheme.typography.titleSmall)
            Text(
                "Choose a resource — you take all of it from every other player",
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
                        count = null,
                        selected = selected == r,
                        enabled = true,
                        size = 40.dp,
                        onClick = { selected = r },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = { selected?.let { onDismiss(); onSubmit(it) } },
                    enabled = selected != null,
                ) { Text("Take") }
            }
        }
    }
}
