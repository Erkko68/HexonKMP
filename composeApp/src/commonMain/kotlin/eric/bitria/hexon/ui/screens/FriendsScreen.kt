package eric.bitria.hexon.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import eric.bitria.hexon.viewmodel.FriendsViewModel

@Composable
fun FriendsScreen(
    friendsViewModel: FriendsViewModel = viewModel { FriendsViewModel() },
    onExitClicked: () -> Unit
) {

}