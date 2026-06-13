package eric.bitria.hexonkmp.ui.components.hud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Spacing
import kotlinx.coroutines.delay

// A live "M:SS" countdown for the current turn, ticked down locally each second.
// [remainingSeconds] is the server's reading (null = no timer → "∞"); [token]
// changes on every server TurnTimer so the countdown restarts for each new turn even
// when consecutive turns share the same duration.
@Composable
fun rememberTurnCountdownLabel(remainingSeconds: Int?, token: Int): String {
    if (remainingSeconds == null) return "∞"
    var seconds by remember(token) { mutableStateOf(remainingSeconds) }
    LaunchedEffect(token) {
        seconds = remainingSeconds
        while (seconds > 0) {
            delay(1000)
            seconds -= 1
        }
    }
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

// Phase + timer pill shown in both game headers.
// [compact] uses smaller padding/typography for portrait; default is landscape size.
@Composable
fun PhasePill(
    phaseLabel: String,
    timeLabel: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val hPad = if (compact) Spacing.md else Spacing.lg
    val vPad = if (compact) Spacing.xs else Spacing.sm
    val hGap = if (compact) Spacing.sm else Spacing.md
    val textStyle = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium
    val iconSize = if (compact) 14.dp else 18.dp
    val letterSpacing = if (compact) 1.sp else 1.5.sp

    Surface(
        modifier = modifier,
        shape = Shapes.pill,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(horizontal = hPad, vertical = vPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(hGap),
        ) {
            Text(
                phaseLabel.uppercase(),
                style = textStyle,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = letterSpacing,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(0.6f),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(
                    Icons.Filled.Timer,
                    contentDescription = "Turn timer",
                    modifier = Modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                )
                Text(
                    timeLabel,
                    style = textStyle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
