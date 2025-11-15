package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.viewmodel.data.ItemCardData
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ItemCards(items: List<ItemCardData>) {
    items.forEach { resource ->
        ItemCard(
            itemCardData = resource
        )
    }
}

// Preview with example data
@Preview(showBackground = true)
@Composable
fun ResourceItemsPreview() {
    val sampleResources = listOf(
        ItemCardData("4","", Icons.Filled.LocalFlorist, "Wool", Color(0xFFBA68C8), Color(0xFFCE93D8)),
        ItemCardData("2","", Icons.Filled.Public, "Ore", Color(0xFFFFF59D), Color(0xFFFFF59D)),
        ItemCardData("5","", Icons.Filled.Park, "Lumber", Color(0xFFFFB74D), Color(0xFFFFCC80)),
        ItemCardData("1","", Icons.Filled.Terrain, "Brick", Color(0xFFB0BEC5), Color(0xFFCFD8DC)),
        ItemCardData("3","", Icons.Filled.Agriculture, "Grain", Color(0xFFFFEB3B), Color(0xFFFFEB3B))
    )
    Row(
        modifier = Modifier
            .width(500.dp)
            .height(50.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ){
        ItemCards(items = sampleResources)
    }
}
