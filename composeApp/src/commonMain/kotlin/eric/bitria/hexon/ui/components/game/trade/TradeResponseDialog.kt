package eric.bitria.hexon.ui.components.game.trade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.game.data.TradeOffer
import eric.bitria.hexon.ui.components.game.IconActionButton
import eric.bitria.hexon.ui.components.game.assets.TradeResourceRow
import eric.bitria.hexon.ui.utils.parseHexColor

@Composable
fun TradeResponseDialog(
    tradeOffer: TradeOffer,
    playerResponses: Map<PlayerId, Boolean?>,
    playerColors: Map<PlayerId, String>,
    onConfirmTrade: (PlayerId) -> Unit,
    onCancelTrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val horizontalSpacing = maxHeight * 0.1f
        val verticalSpacing = maxHeight * 0.03f

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalSpacing, vertical = verticalSpacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
        ) {
            // Given Resources with Arrow Up
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing * 0.5f),
                modifier = Modifier.fillMaxHeight()
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

            // Wanted Resources with Arrow Down
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing * 0.5f),
                modifier = Modifier.fillMaxHeight()
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

            // All players with their response status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing * 0.5f),
                modifier = Modifier.fillMaxHeight()
            ) {
                playerResponses.forEach { (playerId, response) ->
                    val (icon, enabled) = when (response) {
                        true -> Icons.Default.Check to true  // Accepted
                        false -> Icons.Default.Close to false  // Declined
                        null -> Icons.Default.MoreHoriz to false  // Pending
                    }

                    IconActionButton(
                        icon = icon,
                        contentDescription = when (response) {
                            true -> "Confirm trade with player"
                            false -> "Player declined"
                            null -> "Waiting for response"
                        },
                        onClick = { if (enabled) onConfirmTrade(playerId) },
                        enabled = enabled,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = parseHexColor(playerColors[playerId] ?: "#666666"),
                            contentColor = when (response) {
                                true -> Color.Green
                                false -> Color.Red
                                null -> Color.Gray
                            },
                            disabledContainerColor = parseHexColor(playerColors[playerId] ?: "#666666"),
                            disabledContentColor = when (response) {
                                false -> Color.Red
                                else -> Color.Gray
                            }
                        ),
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }

            // Separator
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(0.6f),
                color = Color.White.copy(alpha = 0.2f)
            )

            // Cancel trade button
            IconActionButton(
                icon = Icons.Default.Close,
                contentDescription = "Cancel Trade",
                onClick = onCancelTrade,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Red,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

