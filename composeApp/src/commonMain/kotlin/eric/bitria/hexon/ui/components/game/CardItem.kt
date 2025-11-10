package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ItemCardData(
    val topLeftText: String,
    val topRightText: String,
    val icon: ImageVector,
    val description: String,
    val bgColor: Color,
    val borderColor: Color
)

@Composable
fun ItemCard(
    topLeftText: String,
    topRightText: String,
    icon: ImageVector,
    description: String,
    bgColor: Color,
    borderColor: Color
) {
    val cardShape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(cardShape)
            .background(bgColor.copy(alpha = 0.3f))
            .border(
                1.dp,
                borderColor.copy(alpha = 0.5f),
                cardShape
            )
    ) {
        // Icon always centered
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .align(Alignment.Center)
        )

        // Top-left text
        Text(
            text = topLeftText,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp)
        )

        // Top-right text
        Text(
            text = topRightText,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp)
        )
    }
}