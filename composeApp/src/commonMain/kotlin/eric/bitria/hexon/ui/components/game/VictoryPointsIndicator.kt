package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.ui.utils.TextCanvas
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun VictoryPointsIndicator(
    victoryPoints: Pair<Int, Int>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {

        val height = maxHeight

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(
                    horizontal = height * 0.2f,
                    vertical = height * 0.1f
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.MilitaryTech,
                contentDescription = "Rank",
                tint = Color(0xFFFFEB3B),
                modifier = Modifier
                    .fillMaxHeight(0.75f)
                    .aspectRatio(1f)
            )

            TextCanvas(
                text = "${victoryPoints.first} / ${victoryPoints.second}",
                textStyle = TextStyle(
                    fontSize = (height * 0.5f).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
                modifier = Modifier
                    .padding(end = height * 0.07f, start = height * 0.07f, top = height * 0.05f)
            )
        }
    }
}

@Preview
@Composable
fun VictoryPointsIndicatorPreview(){
    Row(
        modifier = Modifier
            .height(40.dp)
    ) {
        VictoryPointsIndicator(
            victoryPoints = Pair(10, 10)
        )
    }
}