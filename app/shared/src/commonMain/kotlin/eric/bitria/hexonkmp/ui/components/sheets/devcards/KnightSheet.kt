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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import eric.bitria.hexonkmp.ui.theme.Spacing
import hexonkmp.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

// Confirmation sheet for playing the Knight dev card. Informational only —
// the card effect (move robber + steal) plays out on the board after confirming.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnightSheet(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.dev_knight), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(Res.string.knight_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
                Button(onClick = { onDismiss(); onConfirm() }) { Text(stringResource(Res.string.action_play)) }
            }
        }
    }
}
