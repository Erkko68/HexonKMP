package eric.bitria.hexon.ui.components.game.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.game.GamePlayer
import eric.bitria.hexon.game.data.def.BuildingDef
import eric.bitria.hexon.ui.utils.TextCanvas

@Composable
fun BuildingCard(
    building: BuildingDef,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f)
    ) {
        val height = maxHeight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(height * 0.2f))
                .background(Color.White)
        ) {
            // Center icon placeholder
            Icon(
                imageVector = Icons.Default.LocalFlorist,
                contentDescription = building.name,
                tint = Color.Black,
                modifier = Modifier
                    .fillMaxSize(0.65f)
                    .align(Alignment.Center)
            )

            // Top-left: quantity owned
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
