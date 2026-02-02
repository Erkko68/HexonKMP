package eric.bitria.hexon.ui.components.game.assets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.game.GamePlayer
import eric.bitria.hexon.game.data.ResourceId
import eric.bitria.hexon.game.data.def.ResourceDef

@Composable
fun ResourceRow(
    me: GamePlayer?,
    selected: Map<ResourceId, Int>,
    resources: List<ResourceDef>,
    modifier: Modifier = Modifier,
    onClick: (ResourceId) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Only show resources that exist in the player's map
        resources.mapNotNull { resource ->
            me?.resources?.get(resource.id)?.let { count ->
                resource to count
            }
        }.forEach { (resource, count) ->
            val selectedCount = selected[resource.id] ?: 0
            ResourceCard(
                count = count - selectedCount,
                selected = selectedCount,
                resource = resource,
                modifier = Modifier.fillMaxHeight(),
                onClick = { onClick(resource.id) } // Pass resourceId to parent
            )
        }
    }
}