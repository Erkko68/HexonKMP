package eric.bitria.hexonkmp.ui.components.hud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.ui.theme.Spacing
import hexonkmp.app.shared.generated.resources.Res
import hexonkmp.app.shared.generated.resources.vp_abbr
import org.jetbrains.compose.resources.stringResource

@Composable
fun PortraitGameHeader(
    phaseLabel: String,
    timeLabel: String,
    lastRoll: Int?,
    myVictoryPoints: Int,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md).padding(vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                lastRoll?.let { DiceChip(it, compact = true) }
                PhasePill(phaseLabel = phaseLabel, timeLabel = timeLabel, compact = true)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VictoryPointsChip(points = myVictoryPoints, goal = victoryGoal, compact = true)

                IconButton(onClick = onLeave) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Leave game",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}
