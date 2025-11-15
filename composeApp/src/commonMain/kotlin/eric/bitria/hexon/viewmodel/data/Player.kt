package eric.bitria.hexon.viewmodel.data

import androidx.compose.ui.graphics.Color
import eric.bitria.hexon.viewmodel.enums.TradeStatus

data class Player(
    val name: String,
    val tradesEnabled: Boolean,
    val color: Color,
    val status: TradeStatus = TradeStatus.PENDING
)
