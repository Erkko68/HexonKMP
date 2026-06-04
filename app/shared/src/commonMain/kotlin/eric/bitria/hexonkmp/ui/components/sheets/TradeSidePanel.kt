package eric.bitria.hexonkmp.ui.components.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.TradeOffer
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.ui.theme.Spacing

@Composable
fun TradeSidePanel(
    bankRates: Map<Resource, Int>,
    canBankTrade: Boolean,
    hand: ResourceCount,
    me: PlayerId,
    isMyTurn: Boolean,
    playerColor: (PlayerId) -> Color,
    offers: List<TradeOffer>,
    give: ResourceCount,
    receive: ResourceCount,
    onBankTrade: () -> Unit,
    onCycleGive: (Resource) -> Unit,
    onCycleReceive: (Resource) -> Unit,
    onClear: () -> Unit,
    onSubmitPropose: () -> Unit,
    onRespondTrade: (Int, Boolean) -> Unit,
    onFinalizeTrade: (Int, PlayerId) -> Unit,
    onCancelTrade: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(360.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Trade Console", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close trade console")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md)
                    .verticalScroll(rememberScrollState()),
            ) {
                TradeBody(
                    bankRates = bankRates,
                    canBankTrade = canBankTrade,
                    hand = hand,
                    me = me,
                    isMyTurn = isMyTurn,
                    playerColor = playerColor,
                    offers = offers,
                    give = give,
                    receive = receive,
                    onBankTrade = onBankTrade,
                    onCycleGive = onCycleGive,
                    onCycleReceive = onCycleReceive,
                    onClear = onClear,
                    onSubmitPropose = onSubmitPropose,
                    onRespond = onRespondTrade,
                    onFinalize = onFinalizeTrade,
                    onCancel = onCancelTrade,
                )
            }
        }
    }
}
