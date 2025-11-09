package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ItemCards(resources: List<ItemCardData>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = .6f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        resources.forEach { resource ->
            ItemCard(
                count = resource.count,
                icon = resource.icon,
                description = resource.description,
                bgColor = resource.bgColor,
                borderColor = resource.borderColor
            )
        }
    }
}

// Preview with example data
@Preview(showBackground = true)
@Composable
fun ResourceItemsPreview() {
    val sampleResources = listOf(
        ItemCardData("4", Icons.Filled.LocalFlorist, "Wool", Color(0xFFBA68C8), Color(0xFFCE93D8)),
        ItemCardData("2", Icons.Filled.Public, "Ore", Color(0xFFFFF59D), Color(0xFFFFF59D)),
        ItemCardData("5", Icons.Filled.Park, "Lumber", Color(0xFFFFB74D), Color(0xFFFFCC80)),
        ItemCardData("1", Icons.Filled.Terrain, "Brick", Color(0xFFB0BEC5), Color(0xFFCFD8DC)),
        ItemCardData("3", Icons.Filled.Agriculture, "Grain", Color(0xFFFFEB3B), Color(0xFFFFEB3B))
    )

    ItemCards(resources = sampleResources)
}
