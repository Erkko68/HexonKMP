package eric.bitria.hexonkmp.ui.components.hud

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// A prominent circular badge showing the most recent dice total. Red when a 7 is
// rolled (the robber number), otherwise the primary container color.
@Composable
fun RollBadge(roll: Int, modifier: Modifier = Modifier) {
    val isSeven = roll == 7
    Surface(
        modifier = modifier.size(56.dp),
        shape = CircleShape,
        color = if (isSeven) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (isSeven) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("$roll", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
