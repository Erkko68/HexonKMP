package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ControlButton(
    icon: ImageVector,
    color: Color,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    BoxWithConstraints(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxHeight()
                .clickable { onClick() }
                .clip(RoundedCornerShape(maxHeight * 0.08f))
                .background(color)
        ){
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = Color.White,
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }
}

@Preview
@Composable
fun ControlButtonPreview(){
    Column(
        modifier = Modifier
            .height(100.dp)
            .width(100.dp)
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ){
        ControlButton(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            color = Color(0xFF2196F3),
            description = "Play",
            onClick = {},
            modifier = Modifier
                .weight(1f)
        )
        ControlButton(
            icon = Icons.Filled.SwapHoriz,
            color = Color(0xFF4CAF50),
            description = "Play",
            onClick = {},
            modifier = Modifier
                .weight(1f)
        )
    }
}
