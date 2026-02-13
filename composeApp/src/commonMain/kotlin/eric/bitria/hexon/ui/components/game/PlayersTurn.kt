package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import eric.bitria.hexon.ui.utils.parseHexColor

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

    BoxWithConstraints(modifier = modifier) {

        // Since each icon fills max height and is square,
        // the icon size equals maxHeight.
        val iconSize = maxHeight

        // Make spacing relative to icon size (e.g. 20% of width)
        val dynamicSpacing = iconSize * 0.2f

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dynamicSpacing)
        ) {
            allPlayers.forEach { player ->
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    PlayerIcon(
                        color = parseHexColor(player.color),
                        isActive = player.id == activePlayerId,
                        modifier = Modifier.fillMaxSize(0.85f)
                    )
                }
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