package eric.bitria.hexonkmp.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import eric.bitria.hexonkmp.ui.theme.Spacing

// A single build or board action. Build the list in the screen composable and
// pass it to ActionBar — adding or removing an action never touches the bar's
// signature.
data class ActionItem(
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean,
    val selected: Boolean = false,
    val badge: Boolean = false,
    val primary: Boolean = false,
    val onClick: () -> Unit,
)

// A horizontal row of ActionCards driven by a list of ActionItems.
@Composable
fun ActionBar(actions: List<ActionItem>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        actions.forEach { item ->
            ActionCard(
                icon = item.icon,
                label = item.label,
                enabled = item.enabled,
                selected = item.selected,
                badge = item.badge,
                primary = item.primary,
                onClick = item.onClick,
            )
        }
    }
}
