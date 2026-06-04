package eric.bitria.hexonkmp.ui.components.hud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Spacing

// Pill chip showing the most recent dice roll. Uses error colors on a 7 (robber),
// tertiary container otherwise. Padding matches PhasePill so they align in a Row.
// [compact] mirrors PhasePill's compact mode for the portrait header.
@Composable
fun DiceChip(roll: Int, modifier: Modifier = Modifier, compact: Boolean = false) {
    val isSeven = roll == 7
    val bg = if (isSeven) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
    val fg = if (isSeven) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onTertiaryContainer
    val border = if (isSeven) MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
    val hPad = if (compact) Spacing.sm else Spacing.md
    val vPad = if (compact) Spacing.xs else Spacing.sm
    val textStyle = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium

    Surface(
        modifier = modifier,
        shape = Shapes.pill,
        color = bg,
        contentColor = fg,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = hPad, vertical = vPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text("🎲", style = textStyle)
            Text("$roll", style = textStyle, fontWeight = FontWeight.ExtraBold)
        }
    }
}
