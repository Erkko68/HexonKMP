package eric.bitria.hexon.ui.screens.game

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eric.bitria.hexon.render.HexonGameView
import eric.bitria.hexon.ui.theme.HexonTheme
import eric.bitria.hexon.viewmodel.game.GameSceneViewModel
import eric.bitria.hexon.viewmodel.game.SceneState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GameScreen(
    onFriendsClicked: () -> Unit,
    onProfileClicked: () -> Unit,
    gameSceneViewModel: GameSceneViewModel = koinViewModel(),
) {
    val sceneState = gameSceneViewModel.sceneState

    Box(modifier = Modifier.fillMaxSize()) {
        // Shared 3D View (Stays active across scenes)
        HexonGameView(
            modifier = Modifier.fillMaxSize(),
            commands = gameSceneViewModel.gameCommands,
            onGameEvent = { event ->
                gameSceneViewModel.handleGameEvent(event)
            }
        )

        HexonTheme {
            Crossfade(
                targetState = sceneState,
                modifier = Modifier.fillMaxSize()
            ) { state ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (state) {
                        SceneState.MAIN_MENU -> {
                            MainMenuUI(
                                onFriendsClicked = onFriendsClicked,
                                onProfileClicked = onProfileClicked,
                                onMatchmakingClicked = {
                                    gameSceneViewModel.updateSceneState(SceneState.MATCHMAKING)
                                },
                                onCreateLobbyClicked = {
                                    gameSceneViewModel.updateSceneState(SceneState.LOBBY)
                                },
                                isEngineReady = gameSceneViewModel.isEngineReady
                            )
                        }
                        SceneState.MATCHMAKING -> {
                            MatchmakingUI(
                                onExitClicked = {
                                    gameSceneViewModel.updateSceneState(SceneState.MAIN_MENU)
                                }
                            )
                        }
                        SceneState.LOBBY -> {
                            LobbyUI(
                                onExitClicked = {
                                    gameSceneViewModel.updateSceneState(SceneState.MAIN_MENU)
                                }
                            )
                        }
                        SceneState.GAME -> {
                            GameUI(
                                onExitClicked = {
                                    gameSceneViewModel.updateSceneState(SceneState.MAIN_MENU)
                                },
                            )
                        }
                    }
                }
            }

            // Loading Overlay
            if (!gameSceneViewModel.isEngineReady && sceneState == SceneState.MAIN_MENU) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    // Loading...
                }
            }
        }
    }
}
