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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import eric.bitria.hexon.navigation.AuthNavigation
import eric.bitria.hexon.navigation.MainNavigation
import eric.bitria.hexon.ui.theme.HexonTheme
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun App(
    viewModel: AppViewModel = koinViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()

    BoxWithConstraints {
        val sizeClass = WindowSizeClass.calculateFromSize(
            DpSize(maxWidth, maxHeight)
        )
        when (sizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact,
            WindowWidthSizeClass.Medium -> SinglePaneLayout()
            WindowWidthSizeClass.Expanded -> TwoPaneLayout()
        }

    }




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
                        Text("Loading")
                    }
                    SessionState.LOGGED_IN -> {
                        MainNavigation()
                    }
                    SessionState.LOGGED_OUT -> {
                        AuthNavigation()
                    }

                    SessionState.NETWORK_ERROR -> {
                        Text("Network Error")
                    }
                }
            }
        }
    }
}

@Composable
fun SinglePaneLayout(){
    Text("SinglePaneLayout")
}

@Composable
fun TwoPaneLayout(){
    Text("TwoPaneLayout")
}