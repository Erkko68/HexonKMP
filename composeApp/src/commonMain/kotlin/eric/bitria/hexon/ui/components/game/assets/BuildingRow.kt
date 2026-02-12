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
import eric.bitria.hexon.game.data.BuildingId
import eric.bitria.hexon.game.data.def.BuildingDef

@Composable
fun BuildingRow(
    buildings: List<BuildingDef>,
    onClick: (BuildingId) -> Unit = {},
    modifier: Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        buildings.forEach { building ->
            key(building.id) {
                BuildingCard(
                    building = building,
                    onClick = { onClick(building.id) },
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}