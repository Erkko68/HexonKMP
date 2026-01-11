package eric.bitria.hexon.ui.components.game.trade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import eric.bitria.hexon.ui.theme.HexonTheme
import eric.bitria.hexon.viewmodel.data.Player
import eric.bitria.hexon.viewmodel.enums.TradeStatus

@Composable
fun TradePanel(
    players: List<Player>,
    modifier: Modifier = Modifier,
    onPlayerClicked: () -> Unit
) {
    val spacing = HexonTheme.dimensions.spacing

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .border(
                width = spacing.extraSmall * 0.5f,
                color = Color.White.copy(alpha = 0.1f),
                shape = CircleShape
            )
            .padding(horizontal = spacing.small, vertical = spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        players.forEach { player ->
            TradePlayerIcon(
                player = player,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                onClick = onPlayerClicked
            )
        }

        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight(0.6f)
                .padding(horizontal = spacing.extraSmall),
            color = Color.White.copy(alpha = 0.2f)
        )

        TradeBankIcon(
            active = true,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            onClick = {}
        )
    }
}


@Composable
fun TradePlayerIcon(
    player: Player,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val statusIcon = when (player.status) {
        TradeStatus.PENDING -> Icons.Default.HourglassEmpty
        TradeStatus.ACCEPT -> Icons.Default.Check
        TradeStatus.REJECT -> Icons.Default.Close
    }

    val badgeColor = when (player.status) {
        TradeStatus.PENDING -> Color.Yellow
        TradeStatus.ACCEPT -> Color.Green
        TradeStatus.REJECT -> Color.Red
    }

    BoxWithConstraints (
        modifier = modifier
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = player.name,
            modifier = Modifier.fillMaxSize(),
            tint = player.color
        )

        // Status Badge - Prettier with ring
        Box (
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxSize(0.4f)
                .clip(CircleShape)
                .background(color = MaterialTheme.colorScheme.surface)
                .border(maxHeight * 0.05f, player.color, CircleShape)
                .padding(maxHeight * 0.05f),

            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = "${player.status}",
                tint = badgeColor,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun TradeBankIcon(
    active: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = active) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = "Bank",
            modifier = Modifier.fillMaxSize(0.85f),
            tint = if (active) Color.White else Color.Gray.copy(alpha = 0.4f)
        )
    }
}
