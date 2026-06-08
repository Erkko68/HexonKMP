package eric.bitria.hexonkmp.ui.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Spacing
import hexonkmp.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

// Prompt for the player's display name. Shown on first run (no name yet) and when
// the player taps their name to change it. When [onDismiss] is null the dialog is
// mandatory (first run, no Cancel); otherwise it can be dismissed/cancelled.
@Composable
fun NameDialog(
    initial: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    var text by remember { mutableStateOf(initial) }
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        // Take control of the width so the dialog isn't the platform's wide default.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(shape = Shapes.card, modifier = Modifier.width(300.dp)) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    stringResource(Res.string.choose_your_name),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text(stringResource(Res.string.player_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (onDismiss != null) {
                        OutlinedButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
                    }
                    Button(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                        Text(stringResource(Res.string.action_continue))
                    }
                }
            }
        }
    }
}
