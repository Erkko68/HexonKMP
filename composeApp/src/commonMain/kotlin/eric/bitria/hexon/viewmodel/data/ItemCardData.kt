package eric.bitria.hexon.viewmodel.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class ItemCardData(
    val topLeftText: String,
    val topRightText: String,
    val icon: ImageVector,
    val description: String,
    val color: Color,
)