package eric.bitria.hexonkmp.ui.components.hud

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.model.PlayerId

// Stable per-player colors + labels, keyed by seat order. Mirrors the 3D board's
// palette (ResourceColors.forPlayer) so a player reads the same color everywhere.
// Callers resolve color/label once and pass them as plain values to composables.
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

// A player's avatar: a colored, rounded token with a person icon.
// [color] and [label] are pre-resolved by the caller via PlayerVisuals.
@Composable
fun PlayerToken(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
) {
    val onColor = if (color.luminance() > 0.6f) Color.Black else Color.White
    Card(
        modifier = modifier.size(size.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color, contentColor = onColor),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Person,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(0.58f),
            )
        }
    }
}
