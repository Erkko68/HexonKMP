package eric.bitria.hexonkmp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.ui.theme.Spacing

// The local player's development cards, shown as small labelled chips above the
// resource cards. Cards are grouped by type with a count. A Knight chip is
// tappable (plays it) when [playableKnights] > 0 — i.e. it's your Play turn, you
// haven't already played a card this turn, and the knight wasn't bought this turn.
// The other card types aren't playable yet (future slices); VP cards never are.
@Composable
fun DevCardHand(
    cards: Map<DevCard, Int>,
    playableKnights: Int,
    onPlayKnight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cards.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        // Stable display order.
        for (card in DevCard.entries) {
            val count = cards[card] ?: 0
            if (count == 0) continue
            val playable = card == DevCard.KNIGHT && playableKnights > 0
            DevChip(label = card.label, count = count, playable = playable, onClick = onPlayKnight)
        }
    }
}

@Composable
private fun DevChip(label: String, count: Int, playable: Boolean, onClick: () -> Unit) {
    val colors = CardDefaults.cardColors(
        containerColor = if (playable) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (playable) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val content: @Composable () -> Unit = {
        Text(
            if (count > 1) "$label ×$count" else label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
    if (playable) Card(onClick = onClick, colors = colors) { content() }
    else Card(colors = colors) { content() }
}

private val DevCard.label: String
    get() = when (this) {
        DevCard.KNIGHT -> "Knight"
        DevCard.VICTORY_POINT -> "VP"
        DevCard.ROAD_BUILDING -> "Roads"
        DevCard.YEAR_OF_PLENTY -> "Plenty"
        DevCard.MONOPOLY -> "Mono"
    }
