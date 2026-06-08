package eric.bitria.hexonkmp

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.svg.SvgDecoder
import eric.bitria.hexonkmp.di.appModule
import eric.bitria.hexonkmp.ui.screens.GameScreen
import eric.bitria.hexonkmp.ui.theme.AppTheme
import io.github.erkko68.filament.compose.rememberFilamentEngine
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }

    KoinApplication(configuration = koinConfiguration { modules(appModule()) }) {
        AppTheme {
            val engine = rememberFilamentEngine()
            GameScreen(engine = engine)
        }
    }
}
