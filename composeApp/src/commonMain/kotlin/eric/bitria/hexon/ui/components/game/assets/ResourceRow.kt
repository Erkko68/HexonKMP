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
import eric.bitria.hexon.game.data.def.ResourceDef

@Composable
fun ResourceRow(
    resources: List<ResourceDef>,
    modifier: Modifier = Modifier,
    onClick: (ResourceId) -> Unit = {}
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
            resources.forEach { resource ->
                key(resource.id) {
                    ResourceCard(
                        count = 0,
                        resource = resource.id,
                        modifier = Modifier.fillMaxHeight(),
                        onClick = { onClick(resource.id) }
                    )
                }
            }
        }
    }
}