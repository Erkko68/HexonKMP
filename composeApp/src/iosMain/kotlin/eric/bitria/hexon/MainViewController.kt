package eric.bitria.hexon

import androidx.compose.ui.window.ComposeUIViewController
import eric.bitria.hexon.viewmodel.GameViewModel
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val viewModel = GameViewModel()
    return ComposeUIViewController { App(viewModel) }
}