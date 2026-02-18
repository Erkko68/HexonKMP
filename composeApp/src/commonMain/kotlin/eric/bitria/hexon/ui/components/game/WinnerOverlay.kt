package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp

@Composable
fun WinnerOverlay(
    playerName: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Exit")
            }
        },
        title = {
            BoxWithConstraints {
                val fontSize = min(maxWidth, maxHeight).value * 0.05f

                Text(
                    text = "$playerName Wins!",
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        },
        text = {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Victory Star",
                tint = Color(0xFFFFD700),
                modifier = Modifier
                    .fillMaxHeight(0.3f)
                    .aspectRatio(1f)
            )
        }
    )
}
