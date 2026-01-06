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
import androidx.navigation.toRoute
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.screens.FriendProfileScreen
import eric.bitria.hexon.ui.screens.FriendsScreen
import eric.bitria.hexon.ui.screens.GameScreen
import eric.bitria.hexon.ui.screens.LoginScreen
import eric.bitria.hexon.ui.screens.MainMenuScreen
import eric.bitria.hexon.ui.screens.ProfileScreen
import eric.bitria.hexon.ui.screens.Screens
import eric.bitria.hexon.ui.screens.SettingsScreen
import eric.bitria.hexon.ui.screens.VerifyScreen

@Composable
fun App(
    navController: NavHostController = rememberNavController()
) {
    HexonTheme {
        NavHost(
            navController = navController,
            startDestination = Screens.Login,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            composable<Screens.Login>(
                enterTransition = { fadeIn(animationSpec = tween(500)) },
                exitTransition = { fadeOut(animationSpec = tween(500)) }
            ) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screens.MainMenu) {
                            popUpTo(Screens.Login) {
                                inclusive = true
                            }
                        }
                    },
                    onNavigateToVerify = { email ->
                        navController.navigate(Screens.Verify(email))
                    }
                )
            }

            composable<Screens.Verify>(
                enterTransition = { fadeIn(animationSpec = tween(500)) },
                exitTransition = { fadeOut(animationSpec = tween(500)) }
            ) { backStackEntry ->
                val verify: Screens.Verify = backStackEntry.toRoute()
                VerifyScreen(
                    email = verify.email,
                    onVerifySuccess = {
                        navController.navigate(Screens.MainMenu) {
                            popUpTo(Screens.Login) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable<Screens.MainMenu>(
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
                    onFriendsClicked = { navController.navigate(Screens.Friends) },
                    onProfileClicked = { navController.navigate(Screens.Profile) },
                    onStartGameClicked = { navController.navigate(Screens.Game) }
                )
            }

            composable<Screens.Profile>(
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
                    onSettingsClicked = { navController.navigate(Screens.Settings) }
                )
            }

            composable<Screens.Friends>(
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
                    onExitClicked = { navController.popBackStack() },
                    onViewProfileClicked = { username ->
                        navController.navigate(Screens.FriendProfile(username = username))
                    }
                )
            }

            composable<Screens.Settings>(
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

            composable<Screens.Game>(
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
                GameScreen(
                    onExitClicked = { navController.popBackStack() },
                    onAboutClicked = { /*navController.navigate(Screens.About)*/ }
                )
            }

            composable<Screens.FriendProfile>(
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
            ) { backStackEntry ->

                val friendProfile: Screens.FriendProfile = backStackEntry.toRoute()
                val username = friendProfile.username

                FriendProfileScreen(
                    username = username,
                    onExitClicked = { navController.popBackStack() }
                )
            }

        }
    }
}