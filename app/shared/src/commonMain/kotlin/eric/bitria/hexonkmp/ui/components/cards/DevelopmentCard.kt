package eric.bitria.hexonkmp.ui.components.cards

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.ui.theme.DevCardPalette
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Spacing

// One dev card type displayed as a small colored chip with an icon and count.
// [playable] highlights it with a primary border and makes it tappable.
@Composable
fun DevelopmentCard(
    card: DevCard,
    count: Int,
    playable: Boolean,
    onPlay: (DevCard) -> Unit,
) {
    val bg = DevCardPalette.color(card)
    val on = if (bg.luminance() > 0.6f) Color.Black else Color.White
    val border = if (playable) Modifier.border(Shapes.activeBorder, MaterialTheme.colorScheme.primary, Shapes.card) else Modifier
    val colors = CardDefaults.cardColors(containerColor = bg, contentColor = on)
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(DevCardPalette.icon(card), contentDescription = DevCardPalette.label(card), modifier = Modifier.size(22.dp))
            Text("$count", style = MaterialTheme.typography.labelMedium)
        }
    }
    if (playable) {
        Card(onClick = { onPlay(card) }, modifier = border, shape = Shapes.card, colors = colors) { body() }
    } else {
        Card(modifier = border, shape = Shapes.card, colors = colors) { body() }
    }
}
