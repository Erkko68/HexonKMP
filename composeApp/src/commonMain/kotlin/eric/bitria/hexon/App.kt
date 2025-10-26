package eric.bitria.hexon

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.screens.FriendsScreen
import eric.bitria.hexon.ui.screens.GameScreen
import eric.bitria.hexon.ui.screens.LoginScreen
import eric.bitria.hexon.ui.screens.MainMenuScreen
import eric.bitria.hexon.ui.screens.ProfileScreen
import eric.bitria.hexon.ui.screens.Screens
import eric.bitria.hexon.ui.screens.SettingsScreen

@Composable
fun App(
    navController: NavHostController = rememberNavController()
) {
    HexonTheme {
        NavHost(
            navController = navController,
            startDestination = Screens.Login.route,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            composable(
                route = Screens.Login.route,
                enterTransition = { fadeIn(animationSpec = tween(500)) },
                exitTransition = { fadeOut(animationSpec = tween(500)) }
            ) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screens.MainMenu.route) {
                            popUpTo(Screens.Login.route) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable(
                route = Screens.MainMenu.route,
                enterTransition = { fadeIn(animationSpec = tween(500)) },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(400)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(400)
                    )
                }
            ) {
                MainMenuScreen(
                    onFriendsClicked = { navController.navigate(Screens.Friends.route) },
                    onProfileClicked = { navController.navigate(Screens.Profile.route) }
                )
            }

            composable(
                route = Screens.Profile.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(400)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(400)
                    )
                }
            ) {
                ProfileScreen(
                    onExitClicked = { navController.popBackStack() },
                    onSettingsClicked = { navController.navigate(Screens.Settings.route) }
                )
            }

            composable(
                route = Screens.Friends.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(400)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(400)
                    )
                }
            ) {
                FriendsScreen(
                    onExitClicked = { navController.popBackStack() }
                )
            }

            composable(
                route = Screens.Settings.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(400)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(400)
                    )
                }
            ) {
                SettingsScreen(
                    onExitClicked = { navController.popBackStack() }
                )
            }

            // Game
            composable(
                route = Screens.Game.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(400)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(400)
                    )
                }
            ) {
                GameScreen()
            }
        }
    }
}