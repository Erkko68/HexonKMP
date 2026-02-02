package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val allPlayers = listOfNotNull(me?.toSnapshot()) + opponents.values.toList()

    if (allPlayers.isEmpty()) return // nothing to show

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = maxWidth
        val maxHeightPx = maxHeight

        val spacing = 8.dp
        var usedWidth = 0.dp

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            allPlayers.forEach { player ->
                // Skip players without a name or color
                if (player.name.isBlank()) return@forEach

                val isActive = player.id == activePlayerId
                val playerColor = parseColorOrDefault(player.color, Color.White)

                // Approximate width: 0.6 * height per character + spacing
                val textWidth = (player.name.length * 0.6f * maxHeightPx.value).dp + spacing

                if (usedWidth + textWidth <= maxWidthPx) {
                    usedWidth += textWidth

                    Box(
                        modifier = Modifier
                            .width(textWidth)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = player.name,
                            color = if (isActive) playerColor else Color.Gray.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1
                        )
                    }
                } else {
                    // Stop rendering remaining players if not enough space
                    return@forEach
                }
            }
        }
    }
}

fun parseColorOrDefault(colorString: String?, default: Color = Color.White): Color {
    if (colorString.isNullOrBlank()) return default
    return try {
        var hex = colorString.removePrefix("#")
        if (hex.length == 6) hex = "FF$hex" // add alpha
        Color(hex.toLong(16))
    } catch (e: Exception) {
        default
    }
}