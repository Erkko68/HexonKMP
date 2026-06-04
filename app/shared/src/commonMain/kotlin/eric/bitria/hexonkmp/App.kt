package eric.bitria.hexonkmp

import androidx.compose.runtime.Composable
import eric.bitria.hexonkmp.di.appModule
import eric.bitria.hexonkmp.ui.screens.GameScreen
import eric.bitria.hexonkmp.ui.theme.AppTheme
import io.github.erkko68.filament.compose.rememberFilamentEngine
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun App() {
    // One Filament engine hoisted for the whole app session and passed to
    // the board. The game is the only screen.
    KoinApplication(configuration = koinConfiguration(declaration = { modules(appModule()) }), content = {
        AppTheme {
            // One Filament engine hoisted for the whole app session and passed to
            // the board. The game is the only screen.
            val engine = rememberFilamentEngine()
            GameScreen(engine = engine)
        }
    })
}
