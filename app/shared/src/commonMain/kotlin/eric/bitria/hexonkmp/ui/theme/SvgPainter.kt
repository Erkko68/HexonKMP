package eric.bitria.hexonkmp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.painter.Painter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.compose.LocalPlatformContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import hexonkmp.app.shared.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberSvgPainter(path: String): Painter {
    val bytes by produceState<ByteArray?>(null, path) {
        value = Res.readBytes(path)
    }
    val context = LocalPlatformContext.current
    return rememberAsyncImagePainter(
        model = ImageRequest.Builder(context).data(bytes).build()
    )
}
