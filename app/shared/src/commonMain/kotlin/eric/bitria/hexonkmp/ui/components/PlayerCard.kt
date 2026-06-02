package eric.bitria.hexonkmp.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.model.PlayerId

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

// A player shown as a colored card with a person icon — same visual language as
// ResourceToken. [current] outlines it (whose turn it is); when not [present]
// the card dims (the player has left the game).
@Composable
fun PlayerCard(
    player: PlayerId,
    players: List<PlayerId>,
    me: PlayerId,
    modifier: Modifier = Modifier,
    current: Boolean = false,
    present: Boolean = true,
    size: Int = 44,
) {
    val color = PlayerVisuals.color(player, players)
    val onColor = if (color.luminance() > 0.6f) Color.Black else Color.White
    val shape = RoundedCornerShape(12.dp)
    val border = if (current) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape) else Modifier

    Card(
        modifier = modifier.size(size.dp).then(border).alpha(if (present) 1f else 0.35f),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = color, contentColor = onColor),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Person,
                contentDescription = PlayerVisuals.label(player, players, me),
                modifier = Modifier.fillMaxSize(0.58f),
            )
        }
    }
}
