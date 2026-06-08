package eric.bitria.hexonkmp.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eric.bitria.hexonkmp.core.game.model.DevCard
import eric.bitria.hexonkmp.ui.theme.Spacing
import eric.bitria.hexonkmp.ui.theme.Tokens
import eric.bitria.hexonkmp.ui.theme.rememberSvgPainter
import hexonkmp.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

// The local player's dev cards as a row of DevelopmentCards, one chip per type.
// Cards in [playable] are highlighted and fire onPlay(card) on tap. Any held
// achievements (Longest Road / Largest Army) appear first as yellow chips,
// separated from the dev cards by a vertical divider.
@Composable
fun DevelopmentBar(
    cards: Map<DevCard, Int>,
    playable: Set<DevCard>,
    onPlay: (DevCard) -> Unit,
    hasLongestRoad: Boolean = false,
    hasLargestArmy: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val hasAchievement = hasLongestRoad || hasLargestArmy
    val hasDevCards = cards.values.any { it > 0 }
    if (!hasAchievement && !hasDevCards) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasLongestRoad) {
            AchievementCard(
                painter = rememberSvgPainter("files/icons/svg/ic_road.svg"),
                label = stringResource(Res.string.achievement_longest_road),
            )
        }
        if (hasLargestArmy) {
            AchievementCard(
                painter = rememberSvgPainter("files/icons/svg/ic_dev_knight.svg"),
                label = stringResource(Res.string.achievement_largest_army),
            )
        }
        if (hasAchievement && hasDevCards) {
            VerticalDivider(modifier = Modifier.height(Tokens.tokenMd).padding(horizontal = Spacing.xs))
        }
        for (card in DevCard.entries) {
            val count = cards[card] ?: 0
            if (count == 0) continue
            DevelopmentCard(card = card, count = count, playable = card in playable, onPlay = onPlay)
        }
    }
}
