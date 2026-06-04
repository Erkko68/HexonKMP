package eric.bitria.hexonkmp.ui.components.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.ui.theme.Spacing

// One player's status row, stuck to the left edge. [color] and [label] are
// pre-resolved by the caller via PlayerVisuals. On the player's turn the row
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
            PlayerToken(color, label, size = 40)

            Column {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "$resourceCount - Resources",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "$devCardCount - Dev Cards",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("$victoryPoints", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
