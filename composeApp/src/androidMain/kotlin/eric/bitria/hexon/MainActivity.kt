package eric.bitria.hexon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import eric.bitria.hexon.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by lazy { GameViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(viewModel)
        }
    }
}