package eric.bitria.hexonkmp.ui.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.ui.theme.Shapes
import eric.bitria.hexonkmp.ui.theme.Spacing
import eric.bitria.hexonkmp.ui.theme.Tokens
import hexonkmp.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

// Sheet shown when the robber lands on a tile that has buildings from two or more
// opponents. The roller picks one to steal a card from. Laid out like the resource
// sheets: square player cards in a centered row, one per eligible victim.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StealTargetSheet(
    victims: List<PlayerId>,
    playerColor: (PlayerId) -> Color,
    playerLabel: (PlayerId) -> String,
    cardCount: (PlayerId) -> Int,
    onStealFrom: (PlayerId) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = { /* non-dismissible — player must choose */ }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.steal_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(Res.string.steal_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                victims.forEach { victim ->
                    StealTargetCard(
                        color = playerColor(victim),
                        label = playerLabel(victim),
                        cardCount = cardCount(victim),
                        onClick = { onStealFrom(victim) },
                    )
                }
            }
        }
    }
}

// One eligible victim as a square, tappable player card: colored token with a
// person icon, the player's label, and their public resource-card count.
@Composable
private fun StealTargetCard(
    color: Color,
    label: String,
    cardCount: Int,
    onClick: () -> Unit,
) {
    val onColor = if (color.luminance() > 0.6f) Color.Black else Color.White
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.size(Tokens.tokenLg),
            shape = Shapes.card,
            colors = CardDefaults.cardColors(containerColor = color, contentColor = onColor),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(0.55f),
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        Text(
            stringResource(Res.string.cards_count, cardCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
