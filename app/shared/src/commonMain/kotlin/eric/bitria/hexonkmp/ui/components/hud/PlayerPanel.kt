package eric.bitria.hexonkmp.ui.components.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.ui.theme.Spacing
import eric.bitria.hexonkmp.ui.theme.Tokens

// One player's status row, stuck to the left edge. [color] and [label] are
// pre-resolved by the caller via PlayerPalette. On the player's turn the row
// gets a translucent tint of their color; an absent player's row is dimmed.
@Composable
fun PlayerPanel(
    color: Color,
    label: String,
    resourceCount: Int,
    devCardCount: Int,
    victoryPoints: Int,
    isCurrentTurn: Boolean,
    present: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = if (isCurrentTurn) color.copy(alpha = 0.28f) else Color.Transparent

    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(background)
            .alpha(if (present) 1f else 0.4f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.fillMaxHeight().width(5.dp).background(color))

        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PlayerToken(color, label, size = Tokens.tokenSm)

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                // Single compact line: [hammer] resourceCount  [layers] devCardCount.
                // Icon size derived from the text style so they stay visually aligned
                // with no hardcoded dp.
                val labelStyle = MaterialTheme.typography.labelSmall
                val iconSize = with(LocalDensity.current) { labelStyle.fontSize.toDp() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Icon(
                        Icons.Filled.Build,
                        contentDescription = "Resources",
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "$resourceCount",
                        style = labelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        Icons.Filled.Layers,
                        contentDescription = "Dev cards",
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "$devCardCount",
                        style = labelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text("$victoryPoints", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
