package eric.bitria.hexon

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eric.bitria.hexon.api.SessionState
import eric.bitria.hexon.navigation.AuthNavigation
import eric.bitria.hexon.navigation.MainNavigation
import eric.bitria.hexon.ui.theme.HexonTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    viewModel: AppViewModel = koinViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    HexonTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = sessionState,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300, easing = LinearOutSlowInEasing)) +
                            scaleIn(
                                initialScale = 0.92f,
                                animationSpec = tween(300, easing = LinearOutSlowInEasing)
                            )).togetherWith(
                        fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)) +
                                scaleOut(
                                    targetScale = 1.08f,
                                    animationSpec = tween(300, easing = FastOutLinearInEasing)
                                )
                    )
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
