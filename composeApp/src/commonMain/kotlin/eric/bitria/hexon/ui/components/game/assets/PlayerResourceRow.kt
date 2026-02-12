package eric.bitria.hexon.ui.components.game.assets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.game.GamePlayer
import eric.bitria.hexon.game.data.ResourceId

@Composable
fun PlayerResourceRow(
    me: GamePlayer?,
    selected: Map<ResourceId, Int>,
    modifier: Modifier = Modifier,
    onClick: (ResourceId) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    val resources = me?.resources ?: emptyMap()

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        resources.forEach { (resourceId, count) ->
            key(resourceId) {
                val selectedCount = selected[resourceId] ?: 0
                val remaining = count - selectedCount

                ResourceCard(
                    count = remaining,
                    resource = resourceId,
                    modifier = Modifier.fillMaxHeight(),
                    onClick = { onClick(resourceId) }
                )
            }
        }
    }
}