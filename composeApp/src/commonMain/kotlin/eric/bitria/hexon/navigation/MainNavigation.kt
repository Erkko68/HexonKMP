package eric.bitria.hexon.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import eric.bitria.hexon.ui.screens.GameScreen
import eric.bitria.hexon.ui.screens.MainMenuScreen
import eric.bitria.hexon.ui.screens.Screens
import eric.bitria.hexon.ui.screens.SettingsScreen
import eric.bitria.hexon.ui.screens.account.ChangePasswordScreen
import eric.bitria.hexon.ui.screens.account.DeleteAccountScreen
import eric.bitria.hexon.ui.screens.social.FriendProfileScreen
import eric.bitria.hexon.ui.screens.social.FriendsScreen
import eric.bitria.hexon.ui.screens.social.ProfileScreen

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screens.MainMenu,
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        composable<Screens.MainMenu> {
            MainMenuScreen(
                onFriendsClicked = { navController.navigate(Screens.Friends) },
                onProfileClicked = { navController.navigate(Screens.Profile) },
                onStartGameClicked = { navController.navigate(Screens.Game) }
            )
        }

        composable<Screens.Profile> {
            ProfileScreen(
                onExitClicked = { navController.popBackStack() },
                onSettingsClicked = { navController.navigate(Screens.Settings) }
            )
        }

        composable<Screens.Friends> {
            FriendsScreen(
                onExitClicked = { navController.popBackStack() },
                onViewProfileClicked = { username ->
                    navController.navigate(Screens.FriendProfile(username = username))
                }
            )
        }

        composable<Screens.Settings> {
            SettingsScreen(
                onExitClicked = { navController.popBackStack() },
                onChangePasswordClicked = { navController.navigate(Screens.ChangePassword) },
                onDeleteAccountClicked = { navController.navigate(Screens.DeleteAccount) }
            )
        }

        composable<Screens.DeleteAccount> {
            DeleteAccountScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screens.ChangePassword> {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onForgotPassword = {
                    navController.navigate(Screens.ForgotPasswordGraph)
                }
            )
        }

            forgotPasswordGraph(navController) {
                // After successful reset, the ViewModel calls sessionManager.logout()
                // which triggers App.kt to swap to AuthNavigation.
                // No additional action needed here.
            }

        composable<Screens.Game> {
            GameScreen(
                onExitClicked = { navController.popBackStack() },
                onAboutClicked = { /*navController.navigate(Screens.About)*/ }
            )
        }

        composable<Screens.FriendProfile> { backStackEntry ->
            val friendProfile: Screens.FriendProfile = backStackEntry.toRoute()
            FriendProfileScreen(
                username = friendProfile.username,
                onExitClicked = { navController.popBackStack() }
            )
        }
    }
}
