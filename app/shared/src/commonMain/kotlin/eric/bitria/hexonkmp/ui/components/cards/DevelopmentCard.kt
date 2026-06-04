package eric.bitria.hexonkmp.ui.components.cards

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.ui.theme.DevCardPalette
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Tokens

// One dev card type displayed as a fixed-size colored token with an icon and count.
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
    val borderMod = if (playable) Modifier.border(Shapes.activeBorder, MaterialTheme.colorScheme.primary, Shapes.card) else Modifier
    val cardModifier = Modifier.size(Tokens.tokenMd).then(borderMod)
    val colors = CardDefaults.cardColors(containerColor = bg, contentColor = on)
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                DevCardPalette.icon(card),
                contentDescription = DevCardPalette.label(card),
                modifier = Modifier.fillMaxSize(0.5f),
            )
            Text("$count", style = MaterialTheme.typography.labelMedium)
        }
    }
    if (playable) {
        Card(onClick = { onPlay(card) }, modifier = cardModifier, shape = Shapes.card, colors = colors) { content() }
    } else {
        Card(modifier = cardModifier, shape = Shapes.card, colors = colors) { content() }
    }
}
