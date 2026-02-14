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
import eric.bitria.hexon.game.data.ResourceId

@Composable
fun TradeResourceRow(
    selected: Map<ResourceId, Int>,
    onClick: (ResourceId) -> Unit = {},
    modifier: Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val scrollState = rememberScrollState()
        val dynamicSpacing = maxHeight * 0.1f

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(dynamicSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            selected.forEach { (resourceId, count) ->
                key(resourceId) {
                    ResourceCard(
                        count = count,
                        resource = resourceId,
                        onClick = { onClick(resourceId) },
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        }
    }
}