package eric.bitria.hexonkmp.ui.components

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.core.game.action.BankSwap
import eric.bitria.hexonkmp.core.game.model.PlayerId
import eric.bitria.hexonkmp.core.game.model.ResourceCount
import eric.bitria.hexonkmp.core.game.model.TradeOffer
import eric.bitria.hexonkmp.core.game.model.board.Resource
import eric.bitria.hexonkmp.ui.theme.Spacing

// The trade sheet — a mobile-friendly bottom sheet with two tabs: Bank and
// Players. Bank trades require it to be your turn; the Players tab lets the
// current player propose offers and lets opponents respond to incoming ones.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeSheet(
    ratio: Int,
    hand: ResourceCount,
    me: PlayerId,
    isMyTurn: Boolean,
    players: List<PlayerId>,
    offers: List<TradeOffer>,
    onBankTrade: (List<BankSwap>) -> Unit,
    onProposeTrade: (ResourceCount, ResourceCount) -> Unit,
    onRespondTrade: (Int, Boolean) -> Unit,
    onFinalizeTrade: (Int, PlayerId) -> Unit,
    onCancelTrade: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // Opponents open the sheet only to respond, so land them on the Players tab.
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
                    else -> PlayersTab(me, isMyTurn, players, hand, offers, onProposeTrade, onRespondTrade, onFinalizeTrade, onCancelTrade)
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

    // Resources still affordable after the gives already queued.
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

        // Queued swaps as token -> token rows.
        swaps.forEachIndexed { i, swap ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            ) {
                ResourceToken(swap.give, count = ratio, size = 44)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "for")
                ResourceToken(swap.get, count = 1, size = 44)
                IconButton(onClick = { swaps.removeAt(i) }) {
                    Icon(Icons.Filled.Close, contentDescription = "remove")
                }
            }
        }

        // Give picker: every resource, dimmed/disabled when unaffordable.
        Text("Give", style = MaterialTheme.typography.labelMedium)
        TokenRow {
            Resource.entries.forEach { r ->
                ResourceToken(
                    resource = r,
                    count = hand[r],
                    selected = give == r,
                    enabled = canGive(r),
                    onClick = { give = r; if (get == r) get = null },
                )
            }
        }

        // Get picker: any resource other than the chosen give.
        Text("Receive", style = MaterialTheme.typography.labelMedium)
        TokenRow {
            Resource.entries.forEach { r ->
                ResourceToken(
                    resource = r,
                    selected = get == r,
                    enabled = r != give,
                    onClick = { get = r },
                )
            }
        }

        OutlinedButton(
            onClick = {
                val g = give; val r = get
                if (g != null && r != null) { swaps.add(BankSwap(g, r)); give = null; get = null }
            },
            enabled = give != null && get != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Add swap") }

        Button(
            onClick = { onConfirm(swaps.toList()) },
            enabled = swaps.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (swaps.isEmpty()) "Confirm trade" else "Confirm trade (${swaps.size})") }
    }
}

// --- Players tab: propose (current player) + respond/finalize ---

@Composable
private fun PlayersTab(
    me: PlayerId,
    isMyTurn: Boolean,
    players: List<PlayerId>,
    hand: ResourceCount,
    offers: List<TradeOffer>,
    onPropose: (ResourceCount, ResourceCount) -> Unit,
    onRespond: (Int, Boolean) -> Unit,
    onFinalize: (Int, PlayerId) -> Unit,
    onCancel: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (isMyTurn) {
            // Offers sit on top, newest first, so a freshly-sent offer appears
            // right above the form that created it.
            if (offers.isNotEmpty()) {
                Text("Your offers", style = MaterialTheme.typography.labelMedium)
                offers.asReversed().forEach { offer -> ProposerOfferCard(offer, players, me, onFinalize, onCancel) }
            }
            ProposeForm(hand, onPropose)
        } else {
            val incoming = offers.filter { it.proposer != me }
            if (incoming.isEmpty()) {
                Placeholder("No offers right now")
            } else {
                Text("Offers for you", style = MaterialTheme.typography.labelMedium)
                incoming.asReversed().forEach { offer -> IncomingOfferCard(offer, me, players, hand, onRespond) }
            }
        }
    }
}

@Composable
private fun ProposeForm(hand: ResourceCount, onPropose: (ResourceCount, ResourceCount) -> Unit) {
    var give by remember { mutableStateOf(ResourceCount()) }
    var receive by remember { mutableStateOf(ResourceCount()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Propose a trade", style = MaterialTheme.typography.titleSmall)

        Text("You give", style = MaterialTheme.typography.labelMedium)
        TokenRow {
            Resource.entries.forEach { r ->
                ResourceToken(
                    resource = r,
                    count = give[r].takeIf { it > 0 },
                    selected = give[r] > 0,
                    // Only resources you hold, and not already on the "want" side.
                    enabled = hand[r] > 0 && receive[r] == 0,
                    onClick = { give = give.cycle(r, max = hand[r]) },
                )
            }
        }

        Text("You want", style = MaterialTheme.typography.labelMedium)
        TokenRow {
            Resource.entries.forEach { r ->
                ResourceToken(
                    resource = r,
                    count = receive[r].takeIf { it > 0 },
                    selected = receive[r] > 0,
                    enabled = give[r] == 0, // can't ask for what you're also giving
                    onClick = { receive = receive.cycle(r, max = 9) },
                )
            }
        }

        Button(
            onClick = {
                onPropose(give, receive)
                give = ResourceCount(); receive = ResourceCount()
            },
            enabled = !give.isEmpty && !receive.isEmpty,
        ) { Text("Send offer") }
    }
}

// Proposer's view of one of their offers: shows responses, with a Trade button
// per player who accepted.
@Composable
private fun ProposerOfferCard(
    offer: TradeOffer,
    players: List<PlayerId>,
    me: PlayerId,
    onFinalize: (Int, PlayerId) -> Unit,
    onCancel: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Offer centered in the row; the X sits on the right, balanced by an
            // equal-width spacer on the left so the resources stay truly centered.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp))
                OfferLine(offer.give, offer.receive, modifier = Modifier.weight(1f))
                IconButton(onClick = { onCancel(offer.id) }) {
                    Icon(Icons.Filled.Close, contentDescription = "cancel offer")
                }
            }
            val responders = players.filter { it in offer.responses }
            if (responders.isEmpty()) {
                HorizontalDivider()
                StatusText("Waiting for responses…")
            } else {
                responders.forEach { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        PlayerCard(player, players, me, size = 36)
                        Box(Modifier.weight(1f))
                        if (offer.responses[player] == true) {
                            Button(onClick = { onFinalize(offer.id, player) }) { Text("Trade") }
                        } else {
                            Text(
                                "declined",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// Opponent's view of an incoming offer. The offer is described from the
// proposer's side (gives -> wants); accepting means you provide what they want.
@Composable
private fun IncomingOfferCard(
    offer: TradeOffer,
    me: PlayerId,
    players: List<PlayerId>,
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
            // Proposer card on the left; the offered resources centered beside it.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                PlayerCard(offer.proposer, players, me)
                OfferLine(offer.give, offer.receive, modifier = Modifier.weight(1f))
            }
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
            when {
                myResponse == true -> StatusText("You accepted — waiting for confirmation")
                myResponse == false -> StatusText("You declined")
                !canAccept -> StatusText("You don't have what they want", error = true)
            }
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

// A "give -> receive" line of resource bundles, centered within its width.
@Composable
private fun OfferLine(give: ResourceCount, receive: ResourceCount, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        ResourceBundle(give)
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "for")
        ResourceBundle(receive)
    }
}

@Composable
private fun ResourceBundle(rc: ResourceCount) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Resource.entries.filter { rc[it] > 0 }.forEach { r -> ResourceToken(r, count = rc[r], size = 48) }
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

// Tap-to-cycle a single resource's count: +1 up to [max], wrapping back to 0.
private fun ResourceCount.cycle(resource: Resource, max: Int): ResourceCount {
    val next = if (this[resource] + 1 > max) 0 else this[resource] + 1
    return ResourceCount((amounts + (resource to next)).filterValues { it != 0 })
}
