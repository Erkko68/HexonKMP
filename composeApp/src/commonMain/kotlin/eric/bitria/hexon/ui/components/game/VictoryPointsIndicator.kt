package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.ui.theme.HexonTheme
import eric.bitria.hexon.ui.utils.TextCanvas

@Composable
fun VictoryPointsIndicator(
    victoryPoints: Pair<Int, Int>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {

        val height = maxHeight

        // --- Relative sizing ---
        val borderWidth = height * 0.04f
        val horizontalPadding = height * 0.30f
        val iconSizeFraction = 0.65f
        val textSpacing = height * 0.08f
        val textVerticalOffset = height * 0.05f
        val fontSize = (height * 0.5f).value.sp

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .border(
                    width = borderWidth,
                    color = Color.White.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.MilitaryTech,
                contentDescription = "Victory Points",
                tint = Color(0xFFFFD700),
                modifier = Modifier
                    .fillMaxHeight(iconSizeFraction)
                    .aspectRatio(1f)
            )

            TextCanvas(
                text = "${victoryPoints.first} / ${victoryPoints.second}",
                textStyle = MaterialTheme.typography.labelLarge.copy(
                    fontSize = fontSize,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                ),
                modifier = Modifier
                    .padding(start = textSpacing)
                    .padding(top = textVerticalOffset)
            )
        }
    }
}


@Preview
@Composable
fun VictoryPointsIndicatorPreview(){
    HexonTheme {
        Row(modifier = Modifier.height(40.dp)) {
            VictoryPointsIndicator(victoryPoints = Pair(8, 10))
        }
    }
}
