package eric.bitria.hexon

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import eric.bitria.hexon.client.SessionState
import eric.bitria.hexon.navigation.AuthNavigation
import eric.bitria.hexon.navigation.MainNavigation
import eric.bitria.hexon.ui.theme.HexonTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    viewModel: AppViewModel = koinViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()

    HexonTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = sessionState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { state ->
                when (state) {
                    SessionState.LOADING -> {
                        // Optional: Splash screen or Loading indicator
                    }
                    SessionState.LOGGED_IN -> {
                        MainNavigation()
                    }
                    SessionState.LOGGED_OUT -> {
                        AuthNavigation()
                    }
                }
            }
        }
    }
}
