package eric.bitria.hexon.navigation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import eric.bitria.hexon.ui.screens.Screens
import eric.bitria.hexon.ui.screens.account.ChangePasswordScreen
import eric.bitria.hexon.ui.screens.account.DeleteAccountScreen
import eric.bitria.hexon.ui.screens.account.SettingsScreen
import eric.bitria.hexon.ui.screens.game.GameScreen
import eric.bitria.hexon.ui.screens.social.FriendProfileScreen
import eric.bitria.hexon.ui.screens.social.FriendsScreen
import eric.bitria.hexon.ui.screens.social.ProfileScreen

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screens.Game,
        enterTransition = {
            fadeIn(animationSpec = tween(300, easing = LinearOutSlowInEasing))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300, easing = LinearOutSlowInEasing))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing))
        }
    ) {
        composable<Screens.Game> {
            GameScreen(
                onFriendsClicked = { navController.navigate(Screens.Friends) },
                onProfileClicked = { navController.navigate(Screens.Profile) },
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
                onViewProfileClicked = { userId ->
                    navController.navigate(Screens.FriendProfile(userId = userId))
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

        composable<Screens.FriendProfile> { backStackEntry ->
            val friendProfile: Screens.FriendProfile = backStackEntry.toRoute()
            FriendProfileScreen(
                userId = friendProfile.userId,
                onExitClicked = { navController.popBackStack() }
            )
        }
    }
}
