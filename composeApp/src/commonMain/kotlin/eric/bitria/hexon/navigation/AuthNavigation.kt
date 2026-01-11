package eric.bitria.hexon.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import eric.bitria.hexon.ui.screens.Screens
import eric.bitria.hexon.ui.screens.auth.LoginScreen
import eric.bitria.hexon.ui.screens.auth.VerifyScreen

@Composable
fun AuthNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screens.Login,
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } }
    ) {
        composable<Screens.Login> {
            LoginScreen(
                onNavigateToVerify = { email ->
                    navController.navigate(Screens.Verify(email))
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screens.ForgotPasswordGraph)
                }
            )
        }

        composable<Screens.Verify> { backStackEntry ->
            val verify: Screens.Verify = backStackEntry.toRoute()
            VerifyScreen(email = verify.email)
        }

        forgotPasswordGraph(navController) {
            // Pop the entire graph to return to the existing Login screen
            navController.popBackStack(Screens.ForgotPasswordGraph, inclusive = true)
        }
    }
}
