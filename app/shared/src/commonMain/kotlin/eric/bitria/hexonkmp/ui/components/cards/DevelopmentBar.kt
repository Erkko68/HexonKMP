package eric.bitria.hexonkmp.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.ui.theme.Spacing

// The local player's dev cards as a row of DevelopmentCards, one chip per type.
// Cards in [playable] are highlighted and fire onPlay(card) on tap.
@Composable
fun DevelopmentBar(
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
            DevelopmentCard(card = card, count = count, playable = card in playable, onPlay = onPlay)
        }
    }
}
