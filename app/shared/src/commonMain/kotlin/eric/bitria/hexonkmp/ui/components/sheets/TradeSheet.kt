package eric.bitria.hexonkmp.ui.components.sheets

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.TradeOffer
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.ui.components.hud.PlayerToken
import eric.bitria.hexonkmp.ui.components.cards.ResourceCard
import eric.bitria.hexonkmp.ui.theme.Spacing
import eric.bitria.hexonkmp.ui.theme.Tokens

// The trade sheet (portrait): a single give/receive editor that the player can
// send to the bank or offer to other players, plus the live offers below.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeSheet(
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
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
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

// Shared content for both the portrait sheet and the landscape side panel.
@Composable
internal fun TradeBody(
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
    onRespond: (Int, Boolean) -> Unit,
    onFinalize: (Int, PlayerId) -> Unit,
    onCancel: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (isMyTurn) {
            TradeEditor(
                bankRates, canBankTrade, hand, give, receive,
                onCycleGive, onCycleReceive, onClear, onBankTrade, onSubmitPropose,
            )
            if (offers.isNotEmpty()) {
                HorizontalDivider()
                Text("Your offers", style = MaterialTheme.typography.labelMedium)
                offers.asReversed().forEach { offer ->
                    ProposerOfferCard(offer, playerColor, onFinalize, onCancel)
                }
            }
        } else {
            val incoming = offers.filter { it.proposer != me }
            if (incoming.isEmpty()) {
                Placeholder("No offers right now")
            } else {
                Text("Offers for you", style = MaterialTheme.typography.labelMedium)
                incoming.asReversed().forEach { offer ->
                    IncomingOfferCard(offer, me, playerColor, hand, onRespond)
                }
            }
        }
    }
}

// One give/receive editor + two routes: send to the bank, or offer to players.
@Composable
private fun TradeEditor(
    bankRates: Map<Resource, Int>,
    canBankTrade: Boolean,
    hand: ResourceCount,
    give: ResourceCount,
    receive: ResourceCount,
    onCycleGive: (Resource) -> Unit,
    onCycleReceive: (Resource) -> Unit,
    onClear: () -> Unit,
    onBankTrade: () -> Unit,
    onSubmitPropose: () -> Unit,
) {
    val baseRate = bankRates.values.maxOrNull() ?: 0
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("You give", style = MaterialTheme.typography.labelMedium)
        TokenRow {
            Resource.entries.forEach { r ->
                GiveToken(
                    resource = r,
                    selectedCount = give[r],
                    rate = bankRates[r] ?: baseRate,
                    discounted = (bankRates[r] ?: baseRate) < baseRate,
                    enabled = hand[r] > 0 && receive[r] == 0,
                    onClick = { onCycleGive(r) },
                )
            }
        }

        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "for", modifier = Modifier.size(20.dp))

        Text("You want", style = MaterialTheme.typography.labelMedium)
        TokenRow {
            Resource.entries.forEach { r ->
                ResourceCard(
                    resource = r,
                    count = receive[r].takeIf { it > 0 },
                    selected = receive[r] > 0,
                    enabled = give[r] == 0,
                    size = Tokens.tokenSm,
                    onClick = { onCycleReceive(r) },
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Button(
                onClick = onBankTrade,
                enabled = canBankTrade,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Bank", maxLines = 1)
            }
            Button(
                onClick = onSubmitPropose,
                enabled = !give.isEmpty && !receive.isEmpty,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Offer", maxLines = 1)
            }
        }
        OutlinedButton(onClick = onClear, enabled = !give.isEmpty || !receive.isEmpty) { Text("Clear") }
    }
}

// A give-side resource token with its bank ratio shown beneath (highlighted when
// a port discounts it).
@Composable
private fun GiveToken(
    resource: Resource,
    selectedCount: Int,
    rate: Int,
    discounted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ResourceCard(
            resource = resource,
            count = selectedCount.takeIf { it > 0 },
            selected = selectedCount > 0,
            enabled = enabled,
            size = Tokens.tokenSm,
            onClick = onClick,
        )
        Text(
            "$rate:1",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (discounted) FontWeight.Bold else FontWeight.Normal,
            color = if (discounted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Proposer's view of one of their live offers. Responders are iterated from the
// offer's response map directly (insertion order = arrival order).
@Composable
private fun ProposerOfferCard(
    offer: TradeOffer,
    playerColor: (PlayerId) -> Color,
    onFinalize: (Int, PlayerId) -> Unit,
    onCancel: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp))
                OfferLine(offer.give, offer.receive, modifier = Modifier.weight(1f))
                IconButton(onClick = { onCancel(offer.id) }) {
                    Icon(Icons.Filled.Close, contentDescription = "cancel offer")
                }
            }
            HorizontalDivider()
            if (offer.responses.isEmpty()) {
                StatusText("Waiting for responses…")
            } else {
                offer.responses.forEach { (player, accepted) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
                    ) {
                        PlayerToken(playerColor(player), "Player", size = Tokens.tokenSm)
                        if (accepted) {
                            Button(onClick = { onFinalize(offer.id, player) }) { Text("Trade") }
                        } else {
                            Button(onClick = {}, enabled = false) { Text("Declined") }
                        }
                    }
                }
            }
        }
    }
}

// Opponent's view of an incoming offer.
@Composable
private fun IncomingOfferCard(
    offer: TradeOffer,
    me: PlayerId,
    playerColor: (PlayerId) -> Color,
    hand: ResourceCount,
    onRespond: (Int, Boolean) -> Unit,
) {
    val myResponse = offer.responses[me]
    val canAccept = hand.covers(offer.receive)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OfferLine(
                offer.give, offer.receive,
                leading = { PlayerToken(playerColor(offer.proposer), "Proposer", size = Tokens.tokenMd) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(
                    onClick = { onRespond(offer.id, true) },
                    enabled = canAccept && myResponse != true,
                ) { Text("Accept") }
                OutlinedButton(
                    onClick = { onRespond(offer.id, false) },
                    enabled = myResponse != false,
                ) { Text("Decline") }
            }
            if (!canAccept) StatusText("You don't have what they want", error = true)
        }
    }
}

@Composable
private fun StatusText(text: String, error: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun OfferLine(
    give: ResourceCount,
    receive: ResourceCount,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        if (leading != null) {
            leading()
            Text(":", style = MaterialTheme.typography.titleMedium)
        }
        ResourceBundle(give)
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "for")
        ResourceBundle(receive)
    }
}

@Composable
private fun ResourceBundle(rc: ResourceCount) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Resource.entries.filter { rc[it] > 0 }.forEach { r -> ResourceCard(r, count = rc[r]) }
    }
}

@Composable
private fun Placeholder(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = Spacing.xxl), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TokenRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
    ) { content() }
}
