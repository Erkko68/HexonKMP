package eric.bitria.hexon.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import eric.bitria.hexon.ui.theme.HexonTheme
import eric.bitria.hexon.viewmodel.social.GameHistoryItem

@Composable
fun GameHistoryCard(
    item: GameHistoryItem,
    modifier: Modifier
) {
    val dimensions = HexonTheme.dimensions
    val spacing = dimensions.spacing
    val shapes = dimensions.shapes

    BoxWithConstraints(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = shapes.medium
                )
                .padding(horizontal = spacing.large),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Result and Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Text(
                    text = if (item.won) "WIN" else "LOSS",
                    color = if (item.won) Color(0xFF4ADE80) else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Column {
                    Text(
                        text = item.opponent,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = item.date,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Right Side: LP
            Text(
                text = if (item.points > 0) "+${item.points} LP" else "${item.points} LP",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
