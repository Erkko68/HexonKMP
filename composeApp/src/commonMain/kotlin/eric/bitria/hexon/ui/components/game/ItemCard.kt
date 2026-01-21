package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.ui.utils.TextCanvas
import eric.bitria.hexon.viewmodel.data.ItemCardData

@Composable
fun ItemCard(
    itemCardData: ItemCardData,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .aspectRatio(1f)
    ) {
        val height = maxHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(height * 0.2f))
                .background(itemCardData.color)
        ) {
            // Icon in the center
            Icon(
                imageVector = itemCardData.icon,
                contentDescription = itemCardData.description,
                tint = Color.White,
                modifier = Modifier
                    .fillMaxSize(0.65f)
                    .align(Alignment.Center)
            )

            // Top-left text drawn via Canvas
            TextCanvas(
                text = itemCardData.topLeftText,
                textStyle = TextStyle(
                    fontSize = (height * 0.25f).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = height * 0.07f, top = height * 0.05f)
            )

            // Top-right text drawn via Canvas
            TextCanvas(
                text = itemCardData.topRightText,
                textStyle = TextStyle(
                    fontSize = (height * 0.25f).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = height * 0.07f, top = height * 0.05f)
            )
        }
    }
}

@Preview
@Composable
fun ItemPreview(){
    Box(
        modifier = Modifier
            .background(Color.Black)
            .height(500.dp)
            .width(500.dp)
    ){
        ItemCard(
            itemCardData = ItemCardData(
                "14",
                "11",
                Icons.Filled.LocalFlorist,
                "Wool",
                Color(0xFF71417B)
            )
        )
    }
}