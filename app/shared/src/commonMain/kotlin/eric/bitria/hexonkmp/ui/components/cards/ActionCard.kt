package eric.bitria.hexonkmp.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Tokens

// A unified fixed-size square action button. Covers all action contexts:
//   selected = false → build action (surfaceVariant, inactive)
//   selected = true  → build action (primary, build mode armed)
// [badge] draws a notification dot in the top-right corner (trade alerts).
// [icon] is a content slot so callers control rendering — use Image for SVGs
// (no tint) or Icon for Material vectors (auto-tinted to contentColor).
@Composable
fun ActionCard(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    badge: Boolean = false,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    ),
    icon: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.size(Tokens.tokenMd)) {
        Card(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            shape = Shapes.card,
            colors = colors,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center, content = icon)
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
