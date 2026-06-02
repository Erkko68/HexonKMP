package eric.bitria.hexonkmp.ui.board

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import io.github.erkko68.filament.compose.scene.CameraState
import io.github.erkko68.filament.compose.scene.Direction
import io.github.erkko68.filament.compose.scene.Position
import io.github.erkko68.filament.compose.scene.Projection
import kotlin.math.sqrt

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

        // The camera is tilted (eye offset on X and Z), so screen axes don't line
        // up with world X/Z — moving along world axes makes drags feel diagonal.
        // Project the camera's forward onto the ground (Y=0) and derive a screen-
        // aligned right/up basis on that plane, then pan along it.
        //   forward = target - eye = -EYE_OFFSET (on XZ)
        val fx = -EYE_OFFSET.x
        val fz = -EYE_OFFSET.z
        val fLen = sqrt(fx * fx + fz * fz)
        // Screen-up on the ground = forward direction (away from camera).
        val upX = fx / fLen
        val upZ = fz / fLen
        // Screen-right on the ground = up rotated -90° about Y: (z, -x).
        val rightX = upZ
        val rightZ = -upX

        // Drag right (dx>0) should move the world left under the finger, so the
        // content follows the finger -> subtract along right; same for up/forward.
        panX -= (dxScreen * rightX + dyScreen * upX) * worldPerPixel
        panZ -= (dxScreen * rightZ + dyScreen * upZ) * worldPerPixel
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
        // Fixed isometric-style tilt: eye is above and offset on BOTH X and Z so
        // three faces of each cube are visible (top, front, and right) rather
        // than just top+front. Up stays +Y.
        val EYE_OFFSET = Direction(11f, 15f, 13f)
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

// All board gestures in ONE handler so tap / pan / zoom share a single arbiter:
//  - a press that never moves beyond TAP_SLOP and releases quickly -> onTap
//  - single-pointer drag -> pan
//  - two-pointer pinch -> zoom
// Keeping tap here (instead of a separate detectTapGestures pointerInput) avoids
// the two input nodes competing for the same pointer, which silently ate taps.
fun Modifier.boardGestures(
    state: BoardCameraState,
    viewportHeight: () -> Int,
    onTap: (Offset) -> Unit = {},
): Modifier = this
    .pointerInput(state, onTap) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var prevPinch = 0f
            var moved = false
            var pointerCount = 1
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break
                pointerCount = maxOf(pointerCount, pressed.size)
                when {
                    pressed.size >= 2 -> {
                        moved = true
                        val dist = (pressed[0].position - pressed[1].position).getDistance()
                        if (prevPinch > 0f) state.zoomBy(dist / prevPinch)
                        prevPinch = dist
                        event.changes.forEach { it.consume() }
                    }
                    pressed.size == 1 -> {
                        prevPinch = 0f
                        val change = pressed.first()
                        val delta = change.position - change.previousPosition
                        if (delta.getDistance() > TAP_SLOP) moved = true
                        if (moved) {
                            state.panBy(delta.x, delta.y, viewportHeight())
                            change.consume()
                        }
                    }
                }
            }
            // No drag/pinch happened -> treat as a tap at the press position.
            if (!moved && pointerCount == 1) onTap(down.position)
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

private const val TAP_SLOP = 8f
