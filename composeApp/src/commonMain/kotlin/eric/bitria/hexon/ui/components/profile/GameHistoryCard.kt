package eric.bitria.hexon.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.viewmodel.GameHistoryItem

@Composable
fun GameHistoryCard(
    item: GameHistoryItem,
    modifier: Modifier
) {
    BoxWithConstraints(
        modifier = modifier
    ) {
        val height = maxHeight
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(height * 0.1f)
                )
                .padding(height * 0.2f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Result and Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(height * 0.2f)
            ) {
                Text(
                    text = if (item.isWin) "WIN" else "LOSS",
                    color = if (item.isWin) Color(0xFF4ADE80) else Color(0xFFF87171),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Column {
                    Text(
                        text = item.opponents,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.date,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }

            // Right Side: LP
            Text(
                text = if (item.lpChange > 0) "+${item.lpChange} LP" else "${item.lpChange} LP",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}