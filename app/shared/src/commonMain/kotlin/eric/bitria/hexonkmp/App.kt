package eric.bitria.hexonkmp

import androidx.compose.runtime.Composable
import eric.bitria.hexonkmp.di.appModule
import eric.bitria.hexonkmp.ui.layout.AppScaffold
import eric.bitria.hexonkmp.ui.navigation.AppDestination
import eric.bitria.hexonkmp.ui.screens.GameScreen
import eric.bitria.hexonkmp.ui.screens.SettingsScreen
import eric.bitria.hexonkmp.ui.theme.AppTheme
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = { modules(appModule()) }) {
        AppTheme {
            AppScaffold { destination ->
                when (destination) {
                    AppDestination.Game -> GameScreen()
                    AppDestination.Settings -> SettingsScreen()
                }
            }
        }
    }
}
