package eric.bitria.hexonkmp.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.ui.theme.Spacing

// A build action shown as an icon-only square card. Dimmed and non-clickable
// when the action isn't currently available (not your turn, or you can't afford
// it). When [selected] (its build mode is armed), it's highlighted with the
// primary color. [label] is the icon's accessibility description. When [badge] is
// set, a small notification dot is drawn on the corner.
@Composable
fun BuildCard(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    badge: Boolean = false,
) {
    Box(modifier = modifier) {
        Card(
            onClick = onClick,
            enabled = enabled,
            colors = CardDefaults.cardColors(
                containerColor = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.padding(Spacing.sm).size(32.dp),
            )
        }
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.xs)
                    .size(12.dp)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .background(MaterialTheme.colorScheme.error, CircleShape),
            )
        }
    }
}
