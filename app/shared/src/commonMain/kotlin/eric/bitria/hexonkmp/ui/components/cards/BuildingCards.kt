package eric.bitria.hexonkmp.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eric.bitria.hexonkmp.ui.theme.Spacing

// The bottom-center action bar: the build actions (settlement / road / city), the
// buy-development-card action, and trade — each an icon-only BuildCard. Selecting
// a build arms its mode (ghost markers on the board); buy/trade fire immediately.
// State is passed in as plain flags/callbacks so this component knows nothing about
// the game model or the ViewModel.
@Composable
fun BuildingCards(
    canSettlement: Boolean,
    settlementSelected: Boolean,
    canRoad: Boolean,
    roadSelected: Boolean,
    canCity: Boolean,
    citySelected: Boolean,
    canBuyDevCard: Boolean,
    canTrade: Boolean,
    tradeSelected: Boolean,
    tradeBadge: Boolean,
    onSettlement: () -> Unit,
    onRoad: () -> Unit,
    onCity: () -> Unit,
    onBuyDevCard: () -> Unit,
    onTrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        BuildCard(Icons.Filled.Home, "Settlement", enabled = canSettlement, selected = settlementSelected, onClick = onSettlement)
        BuildCard(Icons.Filled.AddRoad, "Road", enabled = canRoad, selected = roadSelected, onClick = onRoad)
        BuildCard(Icons.Filled.LocationCity, "City", enabled = canCity, selected = citySelected, onClick = onCity)
        BuildCard(Icons.Filled.Style, "Buy development card", enabled = canBuyDevCard, onClick = onBuyDevCard)
        BuildCard(Icons.Filled.SwapHoriz, "Trade", enabled = canTrade, selected = tradeSelected, badge = tradeBadge, onClick = onTrade)
    }
}
