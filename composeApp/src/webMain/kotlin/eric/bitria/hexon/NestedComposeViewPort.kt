package eric.bitria.hexon.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.WebElementView
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NestedComposeViewport(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // 1. Capture the latest content lambda in a State object.
    // This allows the inner composition to read the "fresh" content
    // without needing to be recreated itself.
    val currentContent by rememberUpdatedState(content)

    // 2. Create the container
    val container = remember {
        (document.createElement("div") as HTMLDivElement).apply {
            style.apply {
                position = "absolute"
                top = "0px"
                left = "0px"
                width = "100%"
                height = "100%"
                background = "transparent"
            }
        }
    }

    // 3. Mount container to DOM
    WebElementView(
        modifier = modifier,
        factory = { container },
        update = { }
    )

    // 4. Mount the Inner Compose Context
    // NOTE: We only key on 'container'. We do NOT pass 'content' here.
    // The viewport will persist across updates.
    DisposableEffect(container) {
        var composition: Any? = null

        fun mount() {
            if (composition != null) return

            composition = ComposeViewport(container) {
                // This block runs in the NEW inner composition.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                color = Color.Transparent,
                                blendMode = BlendMode.Clear
                            )
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // CRITICAL FIX:
                        // We invoke 'currentContent'. Because this is a State<>,
                        // when the outer 'content' updates, this inner Box will
                        // see the change and recompose automatically.
                        currentContent()
                    }
                }
            }
        }

        val observer = ResizeObserver { entries, _ ->
            val rect = entries[0].contentRect
            if (rect.width > 0 && rect.height > 0) {
                mount()
            }
        }

        observer.observe(container)

        onDispose {
            observer.disconnect()
            composition = null
        }
    }
}

// --- JS Interop ---
private external class ResizeObserver(callback: (Array<ResizeObserverEntry>, ResizeObserver) -> Unit) {
    fun observe(target: Element)
    fun disconnect()
}

private external interface ResizeObserverEntry {
    val contentRect: DOMRectReadOnly
}

private external interface DOMRectReadOnly {
    val width: Double
    val height: Double
}