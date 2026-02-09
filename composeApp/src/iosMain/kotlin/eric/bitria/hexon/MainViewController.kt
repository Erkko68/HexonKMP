package eric.bitria.hexon

import androidx.compose.ui.window.ComposeUIViewController
import eric.bitria.hexon.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = { initKoin }
) {
    App()
}