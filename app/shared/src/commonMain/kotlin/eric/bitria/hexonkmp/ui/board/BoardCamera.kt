package eric.bitria.hexonkmp.ui.board

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import io.github.erkko68.filament.compose.scene.CameraState
import io.github.erkko68.filament.compose.scene.Direction
import io.github.erkko68.filament.compose.scene.Position
import io.github.erkko68.filament.compose.scene.Projection

// A small, self-contained pan + zoom controller for an ANGLED orthographic board
// camera. We don't use Filament's Manipulator (its MAP mode is hardwired
// top-down by upstream Filament's C++, so it can't give an angled view across
// platforms) — instead we drive the CameraState directly here.
//
// The camera keeps a fixed tilt: the eye sits at `target + EYE_OFFSET`, so the
// whole scene (flat hex tiles + cube buildings) is seen under one consistent
// angled parallel projection. Panning slides `target` across the board plane;
// zooming scales the orthographic half-extent.
class BoardCameraState internal constructor(
    private val cameraState: CameraState,
    private val baseHalfExtent: Float,
    initialZoom: Float,
) {
    // Pan offset of the look-at target across the board's X/Z plane.
    var panX by mutableStateOf(0f)
    var panZ by mutableStateOf(0f)

    // 1 = the whole board framed; >1 zooms in, <1 zooms out.
    var zoom by mutableStateOf(initialZoom)

    private var aspect = 1f

    fun setViewport(width: Int, height: Int) {
        if (height > 0) aspect = width.toFloat() / height.toFloat()
        apply()
    }

    fun panBy(dxScreen: Float, dyScreen: Float, viewportHeight: Int) {
        if (viewportHeight <= 0) return
        // Convert screen-pixel drag to world units: the visible world height is
        // 2*halfExtent, so one pixel ≈ (2*halfExtent / viewportHeight) world units.
        val worldPerPixel = (2f * halfExtent()) / viewportHeight
        panX -= dxScreen * worldPerPixel
        panZ -= dyScreen * worldPerPixel
        apply()
    }

    fun zoomBy(factor: Float) {
        zoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        apply()
    }

    private fun halfExtent(): Float = baseHalfExtent / zoom

    // Recompute the camera eye/target/projection from pan + zoom + aspect.
    private fun apply() {
        val target = Position(panX, 0f, panZ)
        cameraState.target = target
        cameraState.eye = Position(panX + EYE_OFFSET.x, EYE_OFFSET.y, panZ + EYE_OFFSET.z)

        val h = halfExtent().toDouble()
        cameraState.projection = if (aspect >= 1f) {
            Projection.Orthographic(-h * aspect, h * aspect, -h, h, 0.1, 100.0)
        } else {
            Projection.Orthographic(-h, h, -h / aspect, h / aspect, 0.1, 100.0)
        }
    }

    companion object {
        // Fixed camera tilt (≈51° above the board). Up stays +Y.
        val EYE_OFFSET = Direction(0f, 16f, 13f)
        const val MIN_ZOOM = 0.5f
        const val MAX_ZOOM = 3f
    }
}

@Composable
fun rememberBoardCameraState(
    cameraState: CameraState,
    baseHalfExtent: Float,
    initialZoom: Float = 1f,
): BoardCameraState = remember(cameraState, baseHalfExtent) {
    BoardCameraState(cameraState, baseHalfExtent, initialZoom).also {
        // Push initial values to the camera.
        it.setViewport(1, 1)
    }
}

// Drag to pan, scroll / pinch to zoom. No rotation (fixed angled view).
fun Modifier.boardGestures(state: BoardCameraState, viewportHeight: () -> Int): Modifier = this
    .pointerInput(state) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var prevPinch = 0f
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break
                when {
                    pressed.size >= 2 -> {
                        val dist = (pressed[0].position - pressed[1].position).getDistance()
                        if (prevPinch > 0f) state.zoomBy(dist / prevPinch)
                        prevPinch = dist
                        event.changes.forEach { it.consume() }
                    }
                    pressed.size == 1 -> {
                        prevPinch = 0f
                        val change = pressed.first()
                        if (change.positionChanged()) {
                            val d = change.positionChange()
                            state.panBy(d.x, d.y, viewportHeight())
                            change.consume()
                        }
                    }
                }
            }
        }
    }
    .pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: continue
                    // Scroll up (negative) zooms in.
                    state.zoomBy(if (delta < 0f) 1.1f else 1f / 1.1f)
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }

// Per-event delta for a single-pointer drag.
private fun androidx.compose.ui.input.pointer.PointerInputChange.positionChange() =
    this.position - this.previousPosition
