package eric.bitria.hexon.ui.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp

@Composable
fun TextCanvas(
    text: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val layoutResult = textMeasurer.measure(AnnotatedString(text), style = textStyle)
    val density = LocalDensity.current

    // Convert text size from pixels to dp for Modifier.size
    val widthDp: Dp
    val heightDp: Dp
    with(density) {
        widthDp = layoutResult.size.width.toDp()
        heightDp = layoutResult.size.height.toDp()
    }

    Canvas(
        modifier = modifier
            .width(widthDp)
            .height(heightDp)
    ) {
        drawText(
            textLayoutResult = layoutResult,
            topLeft = Offset(0f, 0f)
        )
    }
}