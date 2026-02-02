package eric.bitria.hexon.ui.components.game.trade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import eric.bitria.hexon.ui.theme.HexonTheme

@Composable
fun TradePanel(
    onBankTrade: () -> Unit,
    onPlayerTrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = HexonTheme.dimensions.spacing

    BoxWithConstraints(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
            .border(
                width = spacing.extraSmall * 0.5f,
                color = Color.White.copy(alpha = 0.1f),
                shape = CircleShape
            )
            .padding(horizontal = spacing.small, vertical = spacing.extraSmall)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight()
        ) {
            // Button to trade with all players
            TradeIcon(
                icon = Icons.Default.Group,
                description = "Trade with Players",
                onClick = onPlayerTrade
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight(0.6f)
                    .padding(horizontal = spacing.extraSmall),
                color = Color.White.copy(alpha = 0.2f)
            )

            // Button to trade with bank
            TradeIcon(
                icon = Icons.Default.AccountBalance,
                description = "Trade with Bank",
                onClick = onBankTrade
            )
        }
    }
}

@Composable
private fun TradeIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.fillMaxSize(0.85f),
            tint = Color.White
        )
    }
}
