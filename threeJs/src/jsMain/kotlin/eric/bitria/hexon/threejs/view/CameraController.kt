package eric.bitria.hexon.threejs.view

import kotlinx.browser.window
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class CameraController() {

    // --- Configuration ---
    private val frustumSize = 20.0
    private val minZoom = 0.2
    private val maxZoom = 5.0
    private val panSpeed = 1.0

    // Pan state
    private var isPanning = false
    private var lastPanX = 0.0
    private var lastPanY = 0.0

    // Pinch/zoom state
    private var startPinchDistance: Double? = null
    private var startCameraZoom: Double = 1.0

    // Camera
    val camera: dynamic = js("new THREE.OrthographicCamera(0,0,0,0,0.1,1000)")

    // --- Store initial state for reset ---
    private val initialPosition = js("new THREE.Vector3(20, 20, 20)")
    private val initialLookAt = js("new THREE.Vector3(0,0,0)")
    private val initialZoom = 1.0

    init {
        // Set initial bounds
        handleResize(window.innerWidth, window.innerHeight)

        // Isometric setup
        camera.position.copy(initialPosition)
        camera.lookAt(initialLookAt)
        camera.zoom = initialZoom
        camera.updateProjectionMatrix()

        setupInputs()
    }

    // ------------------------------------------------------------
    // Inputs
    // ------------------------------------------------------------
    private fun setupInputs() {
        // --- Wheel zoom ---
        window.addEventListener("wheel", { event ->
            val e = event as WheelEvent
            val zoomFactor = 1.0 - (e.deltaY * 0.001)
            applyZoom(zoomFactor)
        })

        // --- Mouse pan ---
        window.addEventListener("mousedown", { event ->
            val e = event as MouseEvent
            if (e.button.toInt() == 0) {
                isPanning = true
                lastPanX = e.clientX.toDouble()
                lastPanY = e.clientY.toDouble()
            }
        })

        window.addEventListener("mousemove", { event ->
            val e = event as MouseEvent
            if (isPanning) {
                val dx = e.clientX.toDouble() - lastPanX
                val dy = e.clientY.toDouble() - lastPanY
                lastPanX = e.clientX.toDouble()
                lastPanY = e.clientY.toDouble()
                panCamera(dx, dy)
            }
        })

        window.addEventListener("mouseup", { isPanning = false })
        window.addEventListener("mouseleave", { isPanning = false })

        // --- Double click resets camera ---
        window.addEventListener("dblclick", { resetCamera() })

        // --- Touch input ---
        window.addEventListener("touchstart", { event ->
            val e = event as TouchEvent
            when (e.touches.length) {
                1 -> {
                    isPanning = true
                    val t = e.touches.item(0)!!
                    lastPanX = t.pageX.toDouble()
                    lastPanY = t.pageY.toDouble()
                }
                2 -> {
                    isPanning = false
                    startPinchDistance = getTouchDistance(e)
                    startCameraZoom = camera.zoom as Double
                }
            }
        }, js("{ passive: false }")) // passive: false to allow preventDefault

        window.addEventListener("touchmove", { event ->
            val e = event as TouchEvent
            // Pinch
            if (e.touches.length == 2 && startPinchDistance != null) {
                e.preventDefault()
                val ratio = getTouchDistance(e) / startPinchDistance!!
                setZoomRaw(startCameraZoom * ratio)
            }
            // Pan
            else if (e.touches.length == 1 && isPanning) {
                val t = e.touches.item(0)!!
                val dx = t.pageX - lastPanX
                val dy = t.pageY - lastPanY
                lastPanX = t.pageX.toDouble()
                lastPanY = t.pageY.toDouble()
                panCamera(dx, dy)
            }
        }, js("{ passive: false }")) // passive: false to allow preventDefault

        window.addEventListener("touchend", {
            isPanning = false
            startPinchDistance = null
        })
    }

    // ------------------------------------------------------------
    // Core camera math
    // ------------------------------------------------------------
    private fun panCamera(dx: Double, dy: Double) {
        val unitsPerPixel = (frustumSize / (camera.zoom as Double)) / window.innerHeight.toDouble()
        camera.translateX(-dx * unitsPerPixel * panSpeed)
        camera.translateY(dy * unitsPerPixel * panSpeed)
    }

    private fun applyZoom(factor: Double) {
        setZoomRaw((camera.zoom as Double) * factor)
    }

    private fun setZoomRaw(value: Double) {
        val newZoom = max(minZoom, min(maxZoom, value))
        camera.zoom = newZoom
        camera.updateProjectionMatrix()
    }

    private fun getTouchDistance(event: TouchEvent): Double {
        val t1 = event.touches.item(0)!!
        val t2 = event.touches.item(1)!!
        val dx = t1.pageX - t2.pageX
        val dy = t1.pageY - t2.pageY
        return hypot(dx.toDouble(), dy.toDouble())
    }

    // ------------------------------------------------------------
    // Resize
    // ------------------------------------------------------------
    fun handleResize(width: Int, height: Int) {
        val a = width.toDouble() / height.toDouble()
        camera.left = -frustumSize * a / 2
        camera.right = frustumSize * a / 2
        camera.top = frustumSize / 2
        camera.bottom = -frustumSize / 2
        camera.updateProjectionMatrix()
    }

    // ------------------------------------------------------------
    // Reset camera
    // ------------------------------------------------------------
    private fun resetCamera() {
        camera.position.copy(initialPosition)
        camera.lookAt(initialLookAt)
        camera.zoom = initialZoom
        camera.updateProjectionMatrix()
    }
}
