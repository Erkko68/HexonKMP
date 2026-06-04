package eric.bitria.hexonkmp.ui.theme

import androidx.compose.ui.unit.dp

// Standard sizing tokens shared across the whole UI. Using named tokens instead
// of raw dp values keeps every icon/avatar the same size in the same context,
// and makes global rescaling a one-line change.
object Tokens {
    // Avatar / token sizes — use these for PlayerToken, ResourceCard, DevelopmentCard.
    val tokenSm = 36.dp   // compact (responders in trade sheet, tight panels)
    val tokenMd = 48.dp   // default (resource bar, player panel, trade proposer)
    val tokenLg = 64.dp   // prominent (winner dialog)
}
