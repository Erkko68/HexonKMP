package eric.bitria.hexon

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eric.bitria.hexon.ui.screens.GameScreen
import eric.bitria.hexon.ui.screens.LoginScreen
import eric.bitria.hexon.ui.screens.MainMenuScreen
import eric.bitria.hexon.ui.screens.Screens

@Composable
fun App(
    navController: NavHostController = rememberNavController()
) {
    MaterialTheme {
        NavHost(
            navController = navController,
            startDestination = Screens.Login.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(route = Screens.Login.route) {
                LoginScreen(
                    onNavigateToGame = { navController.navigate(Screens.Game.route) }
                )
            }
            composable(route = Screens.MainMenu.route) {
                MainMenuScreen()
            }
            composable(route = Screens.Game.route) {
                GameScreen()
            }
        }
    }
}