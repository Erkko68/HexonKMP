package eric.bitria.hexon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat         // 1. Add this import
import androidx.core.view.WindowInsetsControllerCompat // 2. Add this import

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            App()
        }

        val windowInsetsController =
            WindowInsetsControllerCompat(window, window.decorView)

        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}