package eric.bitria.hexon.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.PopupProperties
import eric.bitria.hexon.ui.theme.HexonTheme

@Composable
fun OptionsButton(
    onExitClicked: () -> Unit,
    onAboutClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val shapes = HexonTheme.dimensions.shapes
    val spacing = HexonTheme.dimensions.spacing

    Box(
        modifier = modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .clickable { expanded = !expanded }
    ){
        Icon(
            imageVector = Icons.Filled.MoreHoriz,
            contentDescription = "More options",
            tint = Color.White,
            modifier = Modifier.fillMaxSize()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .clip(shapes.medium)
                .background(Color.Black.copy(alpha = 0.6f))
                .border(
                    width = spacing.extraSmall * 0.5f,
                    color = Color.White.copy(alpha = 0.1f),
                    shape = shapes.medium
                ),
            properties = PopupProperties(focusable = true, clippingEnabled = false)
        ) {
            DropdownMenuItem(
                text = { Text("Exit", style = MaterialTheme.typography.bodyMedium, color = Color.White) },
                onClick = { expanded = false; onExitClicked() }
            )
            DropdownMenuItem(
                text = { Text("About the Game", style = MaterialTheme.typography.bodyMedium, color = Color.White) },
                onClick = { expanded = false; onAboutClicked() }
            )
        }
    }
}
