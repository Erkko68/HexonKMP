package eric.bitria.hexon.navigation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
        enterTransition = {
            fadeIn(animationSpec = tween(300, easing = LinearOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(300, easing = LinearOutSlowInEasing)
                    )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)) +
                    scaleOut(
                        targetScale = 1.08f,
                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                    )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300, easing = LinearOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 1.08f,
                        animationSpec = tween(300, easing = LinearOutSlowInEasing)
                    )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)) +
                    scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                    )
        }
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
