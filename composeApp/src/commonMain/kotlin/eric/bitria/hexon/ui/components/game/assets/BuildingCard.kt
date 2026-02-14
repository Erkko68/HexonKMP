package eric.bitria.hexon.ui.components.game.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.game.data.def.BuildingDef
import eric.bitria.hexon.ui.utils.AssetIconDisplay
import eric.bitria.hexon.ui.utils.TextCanvas
import eric.bitria.hexon.ui.utils.parseHexColor
import eric.bitria.hexon.ui.utils.rememberAssetData

@Composable
fun BuildingCard(
    building: BuildingDef,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 1. Use the hook to fetch data here
    val iconState by rememberAssetData(building.id)

    // 2. Get Background Color
    val cardBackgroundColor = if (iconState != null) {
        parseHexColor(iconState!!.color).copy(alpha = 0.6f)
    } else {
        Color.Black.copy(alpha = 0.6f)
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        val height = maxHeight

        // Card Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(percent = 20))
                .background(cardBackgroundColor)
        ) {
            // 4. Render the Icon (Stateless)
            AssetIconDisplay(
                data = iconState,
                modifier = Modifier
                    .fillMaxSize(0.65f)
                    .align(Alignment.Center),
                fallbackTint = Color.Gray
            )

            // 5. Quantity Overlay
            TextCanvas(
                text = "",
                textStyle = TextStyle(
                    fontSize = (height * 0.25f).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = height * 0.07f, top = height * 0.05f)
            )
        }
    }
}