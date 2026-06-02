package eric.bitria.hexonkmp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eric.bitria.hexonkmp.ui.theme.Spacing

// Compact turn/phase indicator for the top-left corner: which turn it is and
// whether it's the local player's move. Tinted to draw attention when it's your
// turn.
@Composable
fun TurnIndicator(
    phaseLabel: String,
    statusLabel: String,
    isMyTurn: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isMyTurn) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isMyTurn) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            Text(phaseLabel, style = MaterialTheme.typography.titleMedium)
            Text(statusLabel, style = MaterialTheme.typography.bodySmall)
        }
    }
}
