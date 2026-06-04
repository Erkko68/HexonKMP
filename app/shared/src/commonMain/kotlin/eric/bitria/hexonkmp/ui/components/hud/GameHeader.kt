package eric.bitria.hexonkmp.ui.components.hud

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Spacing

// The top header bar. Left: the app name. Center: a rounded pill with the current
// phase, whose turn it is, and a (placeholder) turn timer. Right: the local
// player's victory-point progress, then a leave button.
//
// The bar's HEIGHT is driven by its text content (the title and the VP column);
// every divider and icon scales to that height via IntrinsicSize.Min +
// fillMaxHeight, so nothing uses a fixed dp size. Drawn on a translucent surface
// so the board stays visible behind it.
@Composable
fun GameHeader(
    phaseLabel: String,
    turnLabel: String,
    timeLabel: String,
    victoryPoints: Int,
    victoryGoal: Int,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            // IntrinsicSize.Min makes the row exactly as tall as its tallest text,
            // so dividers/icons below can fillMaxHeight to match.
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "HexonKMP",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                PhasePill(phaseLabel = phaseLabel, turnLabel = turnLabel, timeLabel = timeLabel)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Victory Points", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$victoryPoints / $victoryGoal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                VerticalDivider(modifier = Modifier.fillMaxHeight(0.8f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Leave game",
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f).clickable(onClick = onLeave),
                )
            }
        }
    }
}

// The centered status pill: "PHASE: <name>   Turn: <player>  |  ⏱ <time>".
@Composable
private fun PhasePill(phaseLabel: String, turnLabel: String, timeLabel: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = Shapes.pill,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text("Phase: $phaseLabel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("Turn: $turnLabel", style = MaterialTheme.typography.labelLarge)
            VerticalDivider(modifier = Modifier.fillMaxHeight(0.8f))
            Icon(Icons.Filled.Timer, contentDescription = "Turn timer", modifier = Modifier.fillMaxHeight().aspectRatio(1f))
            Text(timeLabel, style = MaterialTheme.typography.labelLarge)
        }
    }
}
