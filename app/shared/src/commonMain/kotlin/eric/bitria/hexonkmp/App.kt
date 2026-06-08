package eric.bitria.hexonkmp

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import eric.bitria.hexonkmp.ui.screens.lobby.LobbyViewModel
import eric.bitria.hexonkmp.ui.screens.lobby.MenuScreen
import eric.bitria.hexonkmp.ui.theme.AppTheme
import io.github.erkko68.filament.compose.rememberFilamentEngine
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

private const val MENU_ROUTE = "menu"
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
            // The menu and lobby share one LobbyViewModel (identity + connection +
            // roster live in it); the game has its own. The live connection persists
            // in the shared (singleton) GameRepository across all three.
            val lobbyViewModel = koinViewModel<LobbyViewModel>()

            // Quick cross-fade between screens — the default transition feels sluggish.
            val fade = tween<Float>(durationMillis = 120)
            NavHost(
                navController = navController,
                startDestination = MENU_ROUTE,
                enterTransition = { fadeIn(fade) },
                exitTransition = { fadeOut(fade) },
                popEnterTransition = { fadeIn(fade) },
                popExitTransition = { fadeOut(fade) },
            ) {
                composable(MENU_ROUTE) {
                    MenuScreen(
                        viewModel = lobbyViewModel,
                        onEnterLobby = { navController.navigate(LOBBY_ROUTE) },
                    )
                }
                composable(LOBBY_ROUTE) {
                    LobbyScreen(
                        viewModel = lobbyViewModel,
                        onGameStarted = {
                            navController.navigate(GAME_ROUTE) {
                                popUpTo(LOBBY_ROUTE) { inclusive = true }
                            }
                        },
                        onExit = {
                            navController.navigate(MENU_ROUTE) {
                                popUpTo(MENU_ROUTE) { inclusive = true }
                            }
                        },
                    )
                }
                composable(GAME_ROUTE) {
                    GameScreen(
                        engine = engine,
                        onExit = {
                            navController.navigate(MENU_ROUTE) {
                                popUpTo(MENU_ROUTE) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }
    }
}
