package eric.bitria.hexonkmp.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.model.board.Resource

// A single resource shown as a colored rounded card with the resource icon and
// an optional count. Shared by the hand display and the trade sheet so the look
// is consistent. Optionally tappable + highlightable for selection.
@Composable
fun ResourceToken(
    resource: Resource,
    modifier: Modifier = Modifier,
    count: Int? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    size: Int = 56,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    val border = if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape) else Modifier
    val cardModifier = modifier.size(size.dp).then(border).alpha(if (enabled) 1f else 0.35f)
    val colors = CardDefaults.cardColors(
        containerColor = ResourceVisuals.color(resource),
        contentColor = ResourceVisuals.onColor(resource),
    )

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.Hexagon,
                contentDescription = ResourceVisuals.label(resource),
                modifier = Modifier.size(26.dp),
            )
            if (count != null) {
                Text("$count", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (onClick != null && enabled) {
        Card(onClick = onClick, modifier = cardModifier, shape = shape, colors = colors) { content() }
    } else {
        Card(modifier = cardModifier, shape = shape, colors = colors) { content() }
    }
}
