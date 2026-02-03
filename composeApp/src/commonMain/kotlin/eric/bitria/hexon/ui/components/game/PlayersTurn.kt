package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.game.GamePlayer
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.PlayerSnapshot

@Composable
fun PlayerTurnBar(
    activePlayerId: PlayerId?,
    me: GamePlayer?,
    opponents: Map<PlayerId, PlayerSnapshot>,
    modifier: Modifier = Modifier
) {
    val allPlayers = remember(me, opponents) {
        listOfNotNull(me?.toSnapshot()) + opponents.values
    }

    if (allPlayers.isEmpty()) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        // Packs items to the left (Start) with a small gap
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
    ) {
        allPlayers.forEach { player ->
            // 1. THE CONTAINER (Square)
            // This Box looks at the Row's height, fills it, and forces itself to be a square.
            Box(
                modifier = Modifier
                    .fillMaxHeight()  // Step 1: Match the parent Row's height
                    .aspectRatio(1f), // Step 2: Set width equal to height (Square)
                contentAlignment = Alignment.Center
            ) {
                // 2. THE CONTENT (Icon)
                // This sits inside the square box
                PlayerIcon(
                    color = Color.White, // Replace with dynamic color
                    isActive = player.id == activePlayerId,
                    modifier = Modifier.fillMaxSize(0.85f) // Fill 85% of the square Box
                )
            }
        }
    }
}

@Composable
private fun PlayerIcon(
    color: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape) // Clips the filled square to a circle
            .background(color.copy(alpha = if (isActive) 1f else 0.35f))
            .then(
                if (isActive)
                    Modifier.border(2.dp, Color.White, CircleShape)
                else Modifier
            )
    )
}