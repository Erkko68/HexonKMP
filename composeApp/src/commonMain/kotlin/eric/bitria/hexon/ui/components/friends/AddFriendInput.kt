package eric.bitria.hexon.ui.components.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.theme.HexonTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AddFriendInput(
    onAddFriend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    val dimensions = HexonTheme.dimensions
    val spacing = dimensions.spacing
    val shapes = dimensions.shapes
    val paddingScale = dimensions.paddingScale

    Row(
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = 2.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shapes.medium
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .padding(start = spacing.large),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    Text(
                        text = "Add friend by username",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                innerTextField()
            }
        )

        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onAddFriend(text)
                    text = ""
                }
            },
            modifier = Modifier
                .padding(end = spacing.medium)
                .size(paddingScale * 0.09f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Friend",
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(paddingScale * 0.055f)
            )
        }
    }
}

@Preview
@Composable
fun AddFriendInputPreview() {
    HexonTheme {
        AddFriendInput(
            onAddFriend = {},
            modifier = Modifier
                .clip(HexonTheme.dimensions.shapes.medium)
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )
    }
}
