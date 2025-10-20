package eric.bitria.hexon

import androidx.compose.foundation.layout.fillMaxSize
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
        ) {
            // Login
            composable(route = Screens.Login.route) {
                LoginScreen(
                    onLoginSuccess = { navController.navigate(Screens.MainMenu.route) }
                )
            }
            // MainMenu
            composable(route = Screens.MainMenu.route) {
                MainMenuScreen(
                    onFriendsClicked = { navController.navigate(Screens.Friends.route) },
                    onProfileClicked = { navController.navigate(Screens.Profile.route) }
                )
            }

            composable(route = Screens.Profile.route) {
                ProfileScreen(
                    onExitClicked = { navController.navigate(Screens.MainMenu.route) }
                )
            }
            composable(route = Screens.Friends.route) {
                FriendsScreen(
                    onExitClicked = { navController.navigate(Screens.MainMenu.route) }
                )
            }
            composable(route = Screens.Settings.route) {

            }

            // Game
            composable(route = Screens.Game.route) {
                GameScreen()
            }
        }
    }
}