package eric.bitria.hexonkmp.ui.screens.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Spacing
import hexonkmp.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

// Main-menu dialog for private lobbies: enter a 6-digit code to Join, or Create a
// new lobby. [error] shows inline (e.g. an unknown code) without closing the dialog.
@Composable
fun PrivateLobbyDialog(
    error: String?,
    onJoin: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(shape = Shapes.card, modifier = Modifier.width(300.dp)) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(stringResource(Res.string.private_lobby), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(6) },
                    label = { Text(stringResource(Res.string.lobby_code)) },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = SixDigitCode,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Button(onClick = { onJoin(code) }, enabled = code.length == 6) { Text(stringResource(Res.string.action_join)) }
                    Button(onClick = onCreate) { Text(stringResource(Res.string.action_create)) }
                }
            }
        }
    }
}

// Renders a 6-digit code field as "NNN NNN" while the stored value stays 6 digits.
private val SixDigitCode = VisualTransformation { text ->
    val digits = text.text.take(6)
    val formatted = if (digits.length > 3) "${digits.substring(0, 3)} ${digits.substring(3)}" else digits
    val mapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int) = if (offset <= 3) offset else offset + 1
        override fun transformedToOriginal(offset: Int) = if (offset <= 3) offset else offset - 1
    }
    TransformedText(AnnotatedString(formatted), mapping)
}
