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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.viewmodel.data.Player
import eric.bitria.hexon.viewmodel.enums.TradeStatus
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun TradePanel(
    players: List<Player>,
    modifier: Modifier = Modifier,
    onPlayerClicked: () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = maxWidth * 0.02f, vertical = maxHeight * 0.08f),
            horizontalArrangement = Arrangement.spacedBy(maxWidth * 0.02f),
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
                modifier = Modifier.fillMaxHeight()
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

        // Status Badge
        Box (
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxSize(0.35f)
                .clip(CircleShape)
                .border(maxHeight * 0.02f, player.color, CircleShape)
                .background(color = Color.Black)
                .padding(maxHeight * 0.04f),

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
            .clickable(enabled = active) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val iconTint = if (active) Color.Gray else Color.Gray.copy(alpha = 0.4f)

        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = "Bank",
            modifier = Modifier.fillMaxSize(),
            tint = iconTint
        )
    }
}

@Preview
@Composable
fun TradePlayerIconPreview(){
    TradePlayerIcon(
        player = Player(
            name = "Eric",
            tradesEnabled = true,
            color = Color.Red,
            status = TradeStatus.PENDING
        ),
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f),
        onClick = {}
    )
}