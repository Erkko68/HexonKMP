package eric.bitria.hexonkmp.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.ui.theme.Spacing

// The player's hand as a row of small cards, one per held resource. Each card is
// tinted with that resource's color (ResourceVisuals) and shows its count.
@Composable
fun ResourceCards(hand: ResourceCount, modifier: Modifier = Modifier) {
    val held = Resource.entries.filter { hand[it] > 0 }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (held.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "No resources",
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            held.forEach { res -> ResourceCard(res, hand[res]) }
        }
    }
}

@Composable
private fun ResourceCard(resource: Resource, count: Int) {
    val bg = ResourceVisuals.color(resource)
    val on = ResourceVisuals.onColor(resource)
    Card(colors = CardDefaults.cardColors(containerColor = bg, contentColor = on)) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.Hexagon, contentDescription = ResourceVisuals.label(resource), modifier = Modifier.size(22.dp))
            Text("$count", style = MaterialTheme.typography.labelMedium)
        }
    }
}
