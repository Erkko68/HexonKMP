package eric.bitria.hexonkmp

import androidx.compose.runtime.Composable
import eric.bitria.hexonkmp.di.appModule
import eric.bitria.hexonkmp.ui.layout.AppScaffold
import eric.bitria.hexonkmp.ui.navigation.AppDestination
import eric.bitria.hexonkmp.ui.screens.GameScreen
import eric.bitria.hexonkmp.ui.screens.SettingsScreen
import eric.bitria.hexonkmp.ui.theme.AppTheme
import io.github.erkko68.filament.compose.rememberFilamentEngine
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = { modules(appModule()) }) {
        AppTheme {
            // One Filament engine hoisted for the whole app session, shared by the
            // board across Game/Settings navigation (the heavy object isn't
            // recreated when switching tabs).
            val engine = rememberFilamentEngine()
            AppScaffold { destination ->
                when (destination) {
                    AppDestination.Game -> GameScreen(engine = engine)
                    AppDestination.Settings -> SettingsScreen()
                }
            }
        }
    }
}
