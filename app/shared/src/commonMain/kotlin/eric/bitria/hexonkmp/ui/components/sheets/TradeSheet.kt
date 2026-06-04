package eric.bitria.hexonkmp.ui.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.action.BankSwap
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.TradeOffer
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.ui.components.hud.PlayerToken
import eric.bitria.hexonkmp.ui.components.cards.ResourceCard
import eric.bitria.hexonkmp.ui.theme.Spacing
import eric.bitria.hexonkmp.ui.theme.Tokens

// The trade sheet — a bottom sheet with two tabs: Bank and Players.
// [playerColor] resolves a PlayerId to its seat color for avatar rendering.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeSheet(
    ratio: Int,
    hand: ResourceCount,
    me: PlayerId,
    isMyTurn: Boolean,
    playerColor: (PlayerId) -> Color,
    offers: List<TradeOffer>,
    proposeGive: ResourceCount,
    proposeReceive: ResourceCount,
    onBankTrade: (List<BankSwap>) -> Unit,
    onCycleGive: (Resource) -> Unit,
    onCycleReceive: (Resource) -> Unit,
    onClearPropose: () -> Unit,
    onSubmitPropose: () -> Unit,
    onRespondTrade: (Int, Boolean) -> Unit,
    onFinalizeTrade: (Int, PlayerId) -> Unit,
    onCancelTrade: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(if (isMyTurn) 0 else 1) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Bank") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Players") })
            }
            Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
                when (tab) {
                    0 -> BankTab(ratio, hand, onBankTrade)
                    else -> PlayersTab(
                        me, isMyTurn, playerColor, hand, offers,
                        proposeGive, proposeReceive,
                        onCycleGive, onCycleReceive, onClearPropose, onSubmitPropose,
                        onRespondTrade, onFinalizeTrade, onCancelTrade,
                    )
                }
            }
        }
    }
}

@Composable
private fun BankTab(
    ratio: Int,
    hand: ResourceCount,
    onConfirm: (List<BankSwap>) -> Unit,
) {
    val swaps = remember { mutableStateListOf<BankSwap>() }
    var give by remember { mutableStateOf<Resource?>(null) }
    var get by remember { mutableStateOf<Resource?>(null) }

    val queued = remember(swaps.size) {
        var rc = ResourceCount()
        swaps.forEach { rc += ResourceCount.of(it.give to ratio) }
        rc
    }
    fun canGive(r: Resource) = hand[r] - queued[r] >= ratio

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Trade $ratio : 1 with the bank", style = MaterialTheme.typography.titleSmall)

        swaps.forEachIndexed { i, swap ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            ) {
                ResourceCard(swap.give, count = ratio, size = Tokens.tokenMd)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "for")
                ResourceCard(swap.get, count = 1, size = Tokens.tokenMd)
                IconButton(onClick = { swaps.removeAt(i) }) {
                    Icon(Icons.Filled.Close, contentDescription = "remove")
                }
            }
        }

        Text("Give", style = MaterialTheme.typography.labelMedium)
        TokenRow {
            Resource.entries.forEach { r ->
                ResourceCard(
                    resource = r,
                    count = hand[r] - queued[r],
                    selected = give == r,
                    enabled = canGive(r),
                    onClick = { give = r; if (get == r) get = null },
                )
            }
        }

        Text("Receive", style = MaterialTheme.typography.labelMedium)
        TokenRow {
            Resource.entries.forEach { r ->
                ResourceCard(
                    resource = r,
                    selected = get == r,
                    enabled = r != give,
                    onClick = { get = r },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedButton(
                onClick = {
                    val g = give; val r = get
                    if (g != null && r != null) { swaps.add(BankSwap(g, r)); give = null; get = null }
                },
                enabled = give != null && get != null,
            ) { Text("Add swap") }
            OutlinedButton(
                onClick = { swaps.clear(); give = null; get = null },
                enabled = swaps.isNotEmpty() || give != null || get != null,
            ) { Text("Clear") }
        }

        Button(
            onClick = { onConfirm(swaps.toList()) },
            enabled = swaps.isNotEmpty(),
        ) { Text(if (swaps.isEmpty()) "Confirm trade" else "Confirm trade (${swaps.size})") }
    }
}

@Composable
private fun PlayersTab(
    me: PlayerId,
    isMyTurn: Boolean,
    playerColor: (PlayerId) -> Color,
    hand: ResourceCount,
    offers: List<TradeOffer>,
    proposeGive: ResourceCount,
    proposeReceive: ResourceCount,
    onCycleGive: (Resource) -> Unit,
    onCycleReceive: (Resource) -> Unit,
    onClearPropose: () -> Unit,
    onSubmitPropose: () -> Unit,
    onRespond: (Int, Boolean) -> Unit,
    onFinalize: (Int, PlayerId) -> Unit,
    onCancel: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (isMyTurn) {
            if (offers.isNotEmpty()) {
                Text("Your offers", style = MaterialTheme.typography.labelMedium)
                offers.asReversed().forEach { offer ->
                    ProposerOfferCard(offer, playerColor, onFinalize, onCancel)
                }
            }
            ProposeForm(hand, proposeGive, proposeReceive, onCycleGive, onCycleReceive, onClearPropose, onSubmitPropose)
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

@Composable
private fun ProposeForm(
    hand: ResourceCount,
    give: ResourceCount,
    receive: ResourceCount,
    onCycleGive: (Resource) -> Unit,
    onCycleReceive: (Resource) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Propose a trade", style = MaterialTheme.typography.titleSmall)

        Text("You give", style = MaterialTheme.typography.labelMedium)
        TokenRow {
            Resource.entries.forEach { r ->
                ResourceCard(
                    resource = r,
                    count = give[r].takeIf { it > 0 },
                    selected = give[r] > 0,
                    enabled = hand[r] > 0 && receive[r] == 0,
                    onClick = { onCycleGive(r) },
                )
            }
        }

        Text("You want", style = MaterialTheme.typography.labelMedium)
        TokenRow {
            Resource.entries.forEach { r ->
                ResourceCard(
                    resource = r,
                    count = receive[r].takeIf { it > 0 },
                    selected = receive[r] > 0,
                    enabled = give[r] == 0,
                    onClick = { onCycleReceive(r) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedButton(onClick = onClear, enabled = !give.isEmpty || !receive.isEmpty) { Text("Clear") }
            Button(onClick = onSubmit, enabled = !give.isEmpty && !receive.isEmpty) { Text("Send offer") }
        }
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
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) { content() }
}
