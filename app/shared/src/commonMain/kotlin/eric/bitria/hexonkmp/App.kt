package eric.bitria.hexonkmp

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.svg.SvgDecoder
import eric.bitria.hexonkmp.di.appModule
import eric.bitria.hexonkmp.ui.screens.GameScreen
import eric.bitria.hexonkmp.ui.screens.lobby.LobbyScreen
import eric.bitria.hexonkmp.ui.theme.AppTheme
import io.github.erkko68.filament.compose.rememberFilamentEngine
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

private const val LOBBY_ROUTE = "lobby"
private const val GAME_ROUTE = "game"

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
            val navController = rememberNavController()

            // Two destinations, driven by connection state. Each transition pops the
            // previous destination so its ViewModel is recreated fresh next time; the
            // live connection persists in the shared (singleton) GameRepository.
            NavHost(navController = navController, startDestination = LOBBY_ROUTE) {
                composable(LOBBY_ROUTE) {
                    LobbyScreen(
                        onGameStarted = {
                            navController.navigate(GAME_ROUTE) {
                                popUpTo(LOBBY_ROUTE) { inclusive = true }
                            }
                        },
                    )
                }
                composable(GAME_ROUTE) {
                    GameScreen(
                        engine = engine,
                        onExit = {
                            navController.navigate(LOBBY_ROUTE) {
                                popUpTo(GAME_ROUTE) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }
    }
}
