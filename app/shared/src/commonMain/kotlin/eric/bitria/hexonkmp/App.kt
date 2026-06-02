package eric.bitria.hexonkmp

import androidx.compose.runtime.Composable
import eric.bitria.hexonkmp.di.appModule
import eric.bitria.hexonkmp.ui.screens.GameScreen
import eric.bitria.hexonkmp.ui.theme.AppTheme
import io.github.erkko68.filament.compose.rememberFilamentEngine
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = { modules(appModule()) }) {
        AppTheme {
            // One Filament engine hoisted for the whole app session and passed to
            // the board. The game is the only screen.
            val engine = rememberFilamentEngine()
            GameScreen(engine = engine)
        }
    }
}
