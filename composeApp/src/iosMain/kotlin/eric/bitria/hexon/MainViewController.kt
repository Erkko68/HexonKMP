package eric.bitria.hexon

import androidx.compose.ui.window.ComposeUIViewController
import eric.bitria.hexon.viewmodel.GameViewModel
import eric.bitria.hexon.viewmodel.UIViewModel
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        App()
    }
}