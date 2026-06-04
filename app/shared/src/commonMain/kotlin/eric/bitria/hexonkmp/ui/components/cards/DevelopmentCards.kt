package eric.bitria.hexonkmp.ui.components.cards

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.ui.theme.Spacing

// The local player's development cards as a row of small chips — same visual
// language as ResourceCards. Cards in [playable] get a primary border and fire
// onPlay(card) on tap; others are non-interactive. VP is never playable.
@Composable
fun DevelopmentCards(
    cards: Map<DevCard, Int>,
    playable: Set<DevCard>,
    onPlay: (DevCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cards.isEmpty()) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        for (card in DevCard.entries) {
            val count = cards[card] ?: 0
            if (count == 0) continue
            DevCardChip(
                card = card,
                count = count,
                playable = card in playable,
                onPlay = onPlay,
            )
        }
    }
}

@Composable
private fun DevCardChip(card: DevCard, count: Int, playable: Boolean, onPlay: (DevCard) -> Unit) {
    val bg = DevCardVisuals.color(card)
    val on = if (bg.luminance() > 0.6f) Color.Black else Color.White
    val shape = RoundedCornerShape(12.dp)
    val border = if (playable) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier
    val colors = CardDefaults.cardColors(containerColor = bg, contentColor = on)
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(DevCardVisuals.icon(card), contentDescription = DevCardVisuals.label(card), modifier = Modifier.size(22.dp))
            Text("$count", style = MaterialTheme.typography.labelMedium)
        }
    }
    if (playable) Card(onClick = { onPlay(card) }, modifier = border, shape = shape, colors = colors) { body() }
    else Card(modifier = border, shape = shape, colors = colors) { body() }
}

// Stable per-type color/icon/label for dev cards (mirrors ResourceVisuals).
object DevCardVisuals {
    fun color(card: DevCard): Color = when (card) {
        DevCard.KNIGHT -> Color(0xFFB23A48)
        DevCard.VICTORY_POINT -> Color(0xFFD4A017)
        DevCard.ROAD_BUILDING -> Color(0xFF8D6E63)
        DevCard.YEAR_OF_PLENTY -> Color(0xFF4C9A5B)
        DevCard.MONOPOLY -> Color(0xFF7E57C2)
    }

    fun icon(card: DevCard): ImageVector = when (card) {
        DevCard.KNIGHT -> Icons.Filled.Shield
        DevCard.VICTORY_POINT -> Icons.Filled.Star
        DevCard.ROAD_BUILDING -> Icons.Filled.AddRoad
        DevCard.YEAR_OF_PLENTY -> Icons.Filled.Redeem
        DevCard.MONOPOLY -> Icons.Filled.Paid
    }

    fun label(card: DevCard): String = when (card) {
        DevCard.KNIGHT -> "Knight"
        DevCard.VICTORY_POINT -> "Victory Point"
        DevCard.ROAD_BUILDING -> "Road Building"
        DevCard.YEAR_OF_PLENTY -> "Year of Plenty"
        DevCard.MONOPOLY -> "Monopoly"
    }
}
