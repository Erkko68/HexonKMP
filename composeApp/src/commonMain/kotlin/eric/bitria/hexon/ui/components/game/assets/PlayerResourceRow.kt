package eric.bitria.hexon.ui.components.game.assets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eric.bitria.hexon.game.GamePlayer
import eric.bitria.hexon.game.data.ResourceId

@Composable
fun PlayerResourceRow(
    me: GamePlayer?,
    selected: Map<ResourceId, Int>,
    modifier: Modifier = Modifier,
    onClick: (ResourceId) -> Unit = {}
) {
    BoxWithConstraints(modifier = modifier) {
        val scrollState = rememberScrollState()
        val dynamicSpacing = maxHeight * 0.1f

        val resources = me?.resources ?: emptyMap()

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(dynamicSpacing),
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
}