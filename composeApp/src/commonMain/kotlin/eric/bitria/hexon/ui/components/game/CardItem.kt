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
    val count: String,
    val icon: ImageVector,
    val description: String,
    val bgColor: Color,
    val borderColor: Color
)

@Composable
fun ItemCard(
    count: String,
    icon: ImageVector,
    description: String,
    bgColor: Color,
    borderColor: Color
) {
    val cardShape = RoundedCornerShape(8.dp) // rounded-lg
    Box(
        modifier = Modifier
            .size(48.dp) // w-12 h-12
            .clip(cardShape)
            .background(bgColor.copy(alpha = 0.3f)) // bg-color/30
            .border(
                1.dp,
                borderColor.copy(alpha = 0.5f), // border-color/50
                cardShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(24.dp) // text-2xl
        )
        Text(
            text = count,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp, // text-xs
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = 2.dp) // top-0.5, left-1
        )
    }
}