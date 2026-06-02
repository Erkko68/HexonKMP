package eric.bitria.hexonkmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.ui.theme.Spacing

// Stable per-player colors + labels for the UI, mirroring the 3D board's
// seat-order palette (ResourceColors.forPlayer) so a player reads the same color
// everywhere.
object PlayerVisuals {
    private val palette = listOf(
        Color(0xFFD93333), // red
        Color(0xFF3373D9), // blue
        Color(0xFFF2F2F2), // white
        Color(0xFFF28C26), // orange
    )

    fun color(player: PlayerId, players: List<PlayerId>): Color =
        palette[players.indexOf(player).coerceAtLeast(0) % palette.size]

    // "You" for the local player, otherwise a short seat label (P1, P2, …).
    fun label(player: PlayerId, players: List<PlayerId>, me: PlayerId): String =
        if (player == me) "You" else "P${players.indexOf(player).coerceAtLeast(0) + 1}"
}

// A small rounded pill tinted with the player's color. [current] outlines the
// chip (whose turn it is); when not [present] the chip dims and strikes through
// (the player has left the game).
@Composable
fun PlayerChip(
    player: PlayerId,
    players: List<PlayerId>,
    me: PlayerId,
    modifier: Modifier = Modifier,
    current: Boolean = false,
    present: Boolean = true,
) {
    val color = PlayerVisuals.color(player, players)
    val label = PlayerVisuals.label(player, players, me)
    val shape = RoundedCornerShape(50)
    val onColor = if (color.luminance() > 0.6f) Color.Black else Color.White
    val outline = if (current) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, shape) else Modifier

    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = onColor,
        textDecoration = if (present) null else TextDecoration.LineThrough,
        modifier = modifier
            .then(outline)
            .clip(shape)
            .background(color)
            .alpha(if (present) 1f else 0.4f)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    )
}
