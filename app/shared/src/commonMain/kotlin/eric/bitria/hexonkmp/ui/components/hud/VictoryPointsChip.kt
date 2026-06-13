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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Spacing
import hexonkmp.app.shared.generated.resources.Res
import hexonkmp.app.shared.generated.resources.vp_abbr
import org.jetbrains.compose.resources.stringResource

@Composable
fun VictoryPointsChip(points: Int, goal: Int, modifier: Modifier = Modifier, compact: Boolean = false) {
    val reached = points >= goal
    val bg = if (reached) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
    val fg = if (reached) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onTertiaryContainer
    val border = if (reached) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
    val hPad = if (compact) Spacing.sm else Spacing.md
    val vPad = if (compact) Spacing.xs else Spacing.sm
    val textStyle = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium
    
    val iconSize = with(LocalDensity.current) { textStyle.fontSize.toDp() }

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
            Text("$points / $goal ${stringResource(Res.string.vp_abbr)}", style = textStyle, fontWeight = FontWeight.ExtraBold)
        }
    }
}
