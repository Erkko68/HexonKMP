package eric.bitria.hexon.ui.screens.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eric.bitria.hexon.render.HexonGameView
import eric.bitria.hexon.ui.screens.Screens
import eric.bitria.hexon.ui.theme.HexonTheme
import eric.bitria.hexon.viewmodel.game.GameSceneViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GameScreen(
    onFriendsClicked: () -> Unit,
    onProfileClicked: () -> Unit,
    gameSceneViewModel: GameSceneViewModel = koinViewModel(),
) {
    val nestedNavController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize()) {
        // Shared 3D View (Stays active across the entire nested graph)
        HexonGameView(
            modifier = Modifier.fillMaxSize(),
            commands = gameSceneViewModel.gameCommands,
            onGameEvent = { event ->
                gameSceneViewModel.handleGameEvent(event)
            }
        )

        HexonTheme {
            NavHost(
                navController = nestedNavController,
                startDestination = Screens.GameSubScreens.MainMenu,
                modifier = Modifier.fillMaxSize()
            ) {
                composable<Screens.GameSubScreens.MainMenu> {
                    MainMenuUI(
                        onFriendsClicked = onFriendsClicked,
                        onProfileClicked = onProfileClicked,
                        onMatchmakingClicked = {
                            nestedNavController.navigate(Screens.GameSubScreens.Matchmaking)
                        },
                        onCreateLobbyClicked = {
                            nestedNavController.navigate(Screens.GameSubScreens.Lobby)
                        },
                        isEngineReady = gameSceneViewModel.isEngineReady
                    )
                }

                composable<Screens.GameSubScreens.Matchmaking> {
                    MatchmakingUI(
                        onExitClicked = {
                            nestedNavController.popBackStack()
                        },
                        onGameStarted = {
                            nestedNavController.navigate(Screens.GameSubScreens.Gameplay) {
                                popUpTo<Screens.GameSubScreens.Matchmaking> {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable<Screens.GameSubScreens.Lobby> {
                    LobbyUI(
                        onExitClicked = {
                            nestedNavController.popBackStack()
                        }
                    )
                }

                composable<Screens.GameSubScreens.Gameplay> {
                    GameUI(
                        onExitClicked = {
                            nestedNavController.popBackStack()
                        },
                        viewModel = koinViewModel(
                            parameters = { parametersOf(gameSceneViewModel) }
                        )
                    )
                }
            }
        }
    }
}
