package eric.bitria.hexon.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import eric.bitria.hexon.ui.screens.Screens
import eric.bitria.hexon.ui.screens.account.ForgotPasswordScreen
import eric.bitria.hexon.ui.screens.account.ResetPasswordScreen
import eric.bitria.hexon.viewmodel.account.ForgotPasswordViewModel
import eric.bitria.hexon.viewmodel.account.ResetPasswordViewModel
import org.koin.compose.viewmodel.koinViewModel

fun NavGraphBuilder.forgotPasswordGraph(
    navController: NavHostController,
    onSuccess: () -> Unit
) {
    navigation<Screens.ForgotPasswordGraph>(
        startDestination = Screens.ForgotPassword
    ) {
        composable<Screens.ForgotPassword> { entry ->
            val parentEntry = remember(entry) {
                navController.getBackStackEntry(Screens.ForgotPasswordGraph)
            }
            val viewModel: ForgotPasswordViewModel = koinViewModel(
                viewModelStoreOwner = parentEntry
            )

            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateToReset = {
                    navController.navigate(Screens.ResetPassword())
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screens.ResetPassword> { entry ->
            val parentEntry = remember(entry) {
                navController.getBackStackEntry(Screens.ForgotPasswordGraph)
            }
            val forgotPasswordViewModel: ForgotPasswordViewModel = koinViewModel(
                viewModelStoreOwner = parentEntry
            )
            val resetPasswordViewModel: ResetPasswordViewModel = koinViewModel()

            ResetPasswordScreen(
                viewModel = resetPasswordViewModel,
                email = forgotPasswordViewModel.email,
                onNavigateBack = { navController.popBackStack() },
                onSuccess = onSuccess
            )
        }
    }
}
