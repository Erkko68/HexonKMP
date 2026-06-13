package eric.bitria.hexonkmp.ui.components.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import eric.bitria.hexonkmp.ui.theme.rememberSvgPainter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.ui.theme.Spacing
import eric.bitria.hexonkmp.ui.theme.Tokens
import hexonkmp.app.shared.generated.resources.Res
import hexonkmp.app.shared.generated.resources.vp_abbr
import org.jetbrains.compose.resources.stringResource

@Composable
fun LandscapePlayerPanel(
    color: Color,
    label: String,
    resourceCount: Int,
    devCardCount: Int,
    victoryPoints: Int,
    isCurrentTurn: Boolean,
    present: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundBrush = if (isCurrentTurn) {
        Brush.horizontalGradient(
            colors = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.0f))
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }

    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(backgroundBrush)
            .alpha(if (present) 1f else 0.4f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.fillMaxHeight().width(5.dp).background(color))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PlayerToken(color, label, size = Tokens.tokenSm)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                // Single compact line: [hammer] resourceCount  [layers] devCardCount.
                val labelStyle = MaterialTheme.typography.labelSmall
                val iconSize = with(LocalDensity.current) { labelStyle.fontSize.toDp() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Icon(
                        painter = rememberSvgPainter("files/icons/svg/ic_dev_year_of_plenty.svg"),
                        contentDescription = "Resources",
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "$resourceCount",
                        style = labelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        painter = rememberSvgPainter("files/icons/svg/ic_dev_card.svg"),
                        contentDescription = "Dev cards",
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "$devCardCount",
                        style = labelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text("$victoryPoints ${stringResource(Res.string.vp_abbr)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
