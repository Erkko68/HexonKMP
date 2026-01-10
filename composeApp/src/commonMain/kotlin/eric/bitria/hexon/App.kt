package eric.bitria.hexon

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import eric.bitria.hexon.client.persistence.token.TokenManager
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.screens.GameScreen
import eric.bitria.hexon.ui.screens.MainMenuScreen
import eric.bitria.hexon.ui.screens.Screens
import eric.bitria.hexon.ui.screens.SettingsScreen
import eric.bitria.hexon.ui.screens.account.ResetPasswordScreen
import eric.bitria.hexon.ui.screens.account.ChangePasswordScreen
import eric.bitria.hexon.ui.screens.account.ForgotPasswordScreen
import eric.bitria.hexon.ui.screens.auth.LoginScreen
import eric.bitria.hexon.ui.screens.auth.VerifyScreen
import eric.bitria.hexon.ui.screens.social.FriendProfileScreen
import eric.bitria.hexon.ui.screens.social.FriendsScreen
import eric.bitria.hexon.ui.screens.social.ProfileScreen
import org.koin.compose.koinInject

@Composable
fun App(
    navController: NavHostController = rememberNavController(),
    tokenManager: TokenManager = koinInject()
) {
    val isSessionValid by tokenManager.isSessionValid.collectAsState()

    LaunchedEffect(isSessionValid) {
        if (!isSessionValid) {
            navController.navigate(Screens.Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    HexonTheme {
        NavHost(
            navController = navController,
            startDestination = Screens.Login,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            // Modern Shared Z-Axis transitions
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = LinearOutSlowInEasing)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(300, easing = LinearOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)) +
                        scaleOut(targetScale = 1.08f, animationSpec = tween(300, easing = FastOutLinearInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300, easing = LinearOutSlowInEasing)) +
                        scaleIn(initialScale = 1.08f, animationSpec = tween(300, easing = LinearOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)) +
                        scaleOut(targetScale = 0.92f, animationSpec = tween(300, easing = FastOutLinearInEasing))
            }
        ) {

            composable<Screens.Login> {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screens.MainMenu) {
                            popUpTo(Screens.Login) { inclusive = true }
                        }
                    },
                    onNavigateToVerify = { email ->
                        navController.navigate(Screens.Verify(email))
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(Screens.SendPasswordResetCode)
                    }
                )
            }

            composable<Screens.SendPasswordResetCode> {
                ForgotPasswordScreen(
                    onNavigateToReset = { email ->
                        navController.navigate(Screens.ForgotPassword(email))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screens.ForgotPassword> { backStackEntry ->
                val forgot: Screens.ForgotPassword = backStackEntry.toRoute()
                ResetPasswordScreen(
                    email = forgot.email,
                    onResetSuccess = {
                        navController.navigate(Screens.Login) {
                            popUpTo(Screens.Login) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screens.ResetPassword> {
                ChangePasswordScreen(
                    onSuccess = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screens.Verify> { backStackEntry ->
                val verify: Screens.Verify = backStackEntry.toRoute()
                VerifyScreen(
                    email = verify.email,
                    onVerifySuccess = {
                        navController.navigate(Screens.MainMenu) {
                            popUpTo(Screens.Login) { inclusive = true }
                        }
                    }
                )
            }

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
                    onChangePasswordClicked = { navController.navigate(Screens.ResetPassword) },
                    onLogout = {
                        tokenManager.clearTokens()
                    }
                )
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
}