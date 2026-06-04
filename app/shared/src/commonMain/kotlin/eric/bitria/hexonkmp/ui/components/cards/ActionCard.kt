package eric.bitria.hexonkmp.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Tokens

// A unified fixed-size square action button. Covers all action contexts:
//   primary = false, selected = false → build action (surfaceVariant, inactive)
//   primary = false, selected = true  → build action (primary, build mode armed)
//   primary = true                    → primary action (End Turn, always primary)
// [badge] draws a notification dot in the top-right corner (trade alerts).
@Composable
fun ActionCard(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    selected: Boolean = false,
    badge: Boolean = false,
) {
    val filled = primary || selected
    val containerColor = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor   = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier.size(Tokens.tokenMd)) {
        Card(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            shape = Shapes.card,
            colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.fillMaxSize(0.6f))
            }
        }
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxSize(0.3f)
                    .border(Shapes.activeBorder, MaterialTheme.colorScheme.surface, CircleShape)
                    .background(MaterialTheme.colorScheme.error, CircleShape),
            )
        }
    }
}
