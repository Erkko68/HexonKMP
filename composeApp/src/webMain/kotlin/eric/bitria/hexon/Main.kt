package eric.bitria.hexon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeViewport
import eric.bitria.hexon.di.initKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    ComposeViewport("compose-root") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind{
                    drawRect(
                        size = this.size,
                        color = Color.Transparent,
                        blendMode = BlendMode.Clear
                    )
                }
        ){
            App()
        }
    }
}
