package eric.bitria.hexonkmp.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

// A horizontal snapping "wheel" selector. Items scroll under a fixed centre
// window (the framed slot), and the item nearest the centre is the selection —
// reported via [onSelectedIndex] and rendered emphasized. Tapping an item also
// animates it to the centre. Generic over the item type; [label] renders each
// item and calculates width dynamically per-item.
@Composable
fun <T> ScrollPicker(
    items: List<T>,
    selectedIndex: Int,
    onSelectedIndex: (Int) -> Unit,
    modifier: Modifier = Modifier.width(240.dp),
    label: @Composable (T) -> String,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    val centerIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) selectedIndex
            else {
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2f) - center) }!!.index
            }
        }
    }
    LaunchedEffect(centerIndex) { onSelectedIndex(centerIndex) }

    val slotShape = RoundedCornerShape(10.dp)
    
    // Evaluate labels and pre-measure them to dynamically size the items and the overlay
    val labels = items.map { label(it) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val titleStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    
    val itemWidths = remember(labels, density, titleStyle) {
        labels.map { text ->
            with(density) {
                textMeasurer.measure(text, style = titleStyle).size.width.toDp() + 48.dp
            }
        }
    }

    val itemHeight = remember(labels, density, titleStyle) {
        val maxHeight = labels.maxOfOrNull { text ->
            textMeasurer.measure(text, style = titleStyle).size.height
        } ?: 0
        with(density) { maxHeight.toDp() } + 20.dp
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val sidePaddingStart = if (itemWidths.isNotEmpty()) (maxWidth - itemWidths.first()) / 2 else 0.dp
        val sidePaddingEnd = if (itemWidths.isNotEmpty()) (maxWidth - itemWidths.last()) / 2 else 0.dp

        // Animate the overlay width to match the exact size of the currently centered item
        val targetOverlayWidth = itemWidths.getOrNull(centerIndex) ?: 48.dp
        val animatedOverlayWidth by animateDpAsState(targetValue = targetOverlayWidth)

        // The fixed centre "window": frames the current selection
        Box(
            Modifier
                .width(animatedOverlayWidth)
                .height(itemHeight)
                .clip(slotShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), slotShape)
        )

        LazyRow(
            state = listState,
            flingBehavior = fling,
            contentPadding = PaddingValues(start = sidePaddingStart, end = sidePaddingEnd),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    var isDragging = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull()
                        if (change != null && change.type == PointerType.Mouse) {
                            if (event.type == PointerEventType.Move && change.pressed) {
                                isDragging = true
                                val delta = change.position.x - change.previousPosition.x
                                if (delta != 0f) {
                                    scope.launch { listState.scrollBy(-delta) }
                                }
                            } else if (event.type == PointerEventType.Release) {
                                if (isDragging) {
                                    isDragging = false
                                    scope.launch { listState.centerItem(centerIndex) }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            itemsIndexed(items) { i, item ->
                val selected = i == centerIndex
                Box(
                    modifier = Modifier
                        .width(itemWidths[i])
                        .height(itemHeight)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { scope.launch { listState.centerItem(i) } })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(item),
                        maxLines = 1,
                        style = if (selected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

// Smoothly scroll [index] to the centre of the viewport. Uses a precise pixel
// delta when the item is already visible (the common tap-a-neighbour case), else
// falls back to a coarse scroll-to that the snap fling then settles.
private suspend fun LazyListState.centerItem(index: Int) {
    val target = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (target != null) {
        val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
        animateScrollBy((target.offset + target.size / 2f) - center)
    } else {
        animateScrollToItem(index)
    }
}
