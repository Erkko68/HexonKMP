package eric.bitria.hexon.ui.components.game.trade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.TradeOffer
import eric.bitria.hexon.ui.components.game.IconActionButton
import eric.bitria.hexon.ui.components.game.assets.TradeResourceRow
import eric.bitria.hexon.ui.utils.parseHexColor

@Composable
fun TradeRequest(
    playerId: PlayerId,
    playerColor: String,
    tradeOffer: TradeOffer,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val horizontalSpacing = maxWidth * 0.03f
        val verticalSpacing = maxHeight * 0.03f
        val borderWidth = maxHeight * 0.02f

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalSpacing, vertical = verticalSpacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
        ) {
            // Player Icon
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(parseHexColor(playerColor))
                    .border(borderWidth, Color.White.copy(alpha = 0.5f), CircleShape)
            )

            // Resources Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                TradeResourceRow(
                    selected = tradeOffer.give,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = verticalSpacing / 2)
                        .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                )
                TradeResourceRow(
                    selected = tradeOffer.want,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = verticalSpacing / 2)
                        .background(Color.Green.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                )
            }

            // Buttons Row - Maintain square aspect ratio
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                modifier = Modifier.fillMaxHeight()
            ) {
                IconActionButton(
                    icon = Icons.Default.Check,
                    contentDescription = "Accept Trade",
                    onClick = onAccept,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Green,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxHeight()
                )

                IconActionButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Decline Trade",
                    onClick = onDecline,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Red,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}
