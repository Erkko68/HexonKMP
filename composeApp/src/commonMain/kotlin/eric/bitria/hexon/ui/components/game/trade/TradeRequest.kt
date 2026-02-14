package eric.bitria.hexon.ui.components.game.trade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        val horizontalSpacing = maxWidth * 0.015f
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

            // Separator
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(0.6f),
                color = Color.White.copy(alpha = 0.2f)
            )

            // Given Resources with Arrow Up
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing * 0.5f),
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Given Resources",
                    tint = Color.Red,
                    modifier = Modifier.fillMaxHeight(0.3f)
                )
                TradeResourceRow(
                    selected = tradeOffer.give,
                    modifier = Modifier.fillMaxHeight()
                )
            }

            // Separator
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(0.6f),
                color = Color.White.copy(alpha = 0.2f)
            )

            // Wanted Resources with Arrow Down
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing * 0.5f),
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Wanted Resources",
                    tint = Color.Green,
                    modifier = Modifier.fillMaxHeight(0.3f)
                )
                TradeResourceRow(
                    selected = tradeOffer.want,
                    modifier = Modifier.fillMaxHeight()
                )
            }

            // Separator
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(0.6f),
                color = Color.White.copy(alpha = 0.2f)
            )

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
