package eric.bitria.hexonkmp.ui.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.ui.theme.ResourcePalette
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Tokens

// A single resource shown as a colored token with its icon and an optional count.
// Used by ResourceBar (display-only), TradeSheet (interactive), and DiscardSheet.
@Composable
fun ResourceCard(
    resource: Resource,
    modifier: Modifier = Modifier,
    count: Int? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    size: Dp = Tokens.tokenMd,
    onClick: (() -> Unit)? = null,
) {
    val cardModifier = modifier.size(size).alpha(if (enabled) 1f else 0.35f)
    val colors = CardDefaults.cardColors(
        containerColor = ResourcePalette.color(resource),
        contentColor = ResourcePalette.onColor(resource),
    )
    // The selection ring uses the Card's own border slot so it's drawn with the
    // card's shape and always hugs the rounded corners (a plain Modifier.border
    // can read as a square against the card's bounds).
    val border = if (selected) BorderStroke(Shapes.selectedBorder, MaterialTheme.colorScheme.primary) else null
    val iconFraction = if (count != null) 0.45f else 0.6f
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Filled.Hexagon,
                contentDescription = ResourcePalette.label(resource),
                modifier = Modifier.fillMaxSize(iconFraction),
            )
            if (count != null) {
                Text("$count", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
    // Keep the same node type regardless of enabled — toggling `enabled` on the
    // Card avoids the node swap that can swallow an in-flight press.
    if (onClick != null) {
        Card(onClick = onClick, enabled = enabled, modifier = cardModifier, shape = Shapes.card, colors = colors, border = border) { content() }
    } else {
        Card(modifier = cardModifier, shape = Shapes.card, colors = colors, border = border) { content() }
    }
}
