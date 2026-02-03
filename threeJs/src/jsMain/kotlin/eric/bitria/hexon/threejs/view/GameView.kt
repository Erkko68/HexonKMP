package eric.bitria.hexon.threejs.view

import eric.bitria.hexon.threejs.engine.Engine
import kotlinx.browser.document
import kotlinx.browser.window

class GameView(val engine: Engine) {
    private val canvas = document.getElementById("three-root")
        ?: throw IllegalStateException("Canvas #three-root not found")

    // VISUAL STATE: Scene belongs here
    val scene = js("new THREE.Scene()")

    private val renderer = run {
        val config = js("{}")
        config.canvas = canvas
        config.antialias = true
        js("new THREE.WebGLRenderer(config)")
    }

    private val cameraController = CameraController()

    init {
        // Link Engine to View
        engine.view = this

        setupVisuals()
        setupRenderer()
        animate()
    }

    // PURE VISUAL SETUP
    private fun setupVisuals() {
        scene.background = js("new THREE.Color(0x222222)")

        // GridHelper (Visual)
        val size = 20
        val divisions = 20
        val gridHelper = js("new THREE.GridHelper(size, divisions, 0x888888, 0x444444)")
        scene.add(gridHelper)

        // Lights (Visual)
        val light = js("new THREE.AmbientLight(0xffffff, 1.0)")
        scene.add(light)
    }

    private fun setupRenderer() {
        renderer.setSize(window.innerWidth, window.innerHeight)
        renderer.setPixelRatio(window.devicePixelRatio)

        window.addEventListener("resize", {
            val w = window.innerWidth
            val h = window.innerHeight
            cameraController.handleResize(w, h)
            renderer.setSize(w, h)
        })
    }

    private fun animate() {
        window.requestAnimationFrame { animate() }
        renderer.render(scene, cameraController.camera)
    }

    // VISUAL API (Called by Engine)
    fun renderHex(data: dynamic) {
        console.log("View: Drawing Hex at ${data.q}, ${data.r}")
        // TODO: Create Mesh here
    }

    fun placeBuilding(data: dynamic) {
        console.log("View: Placing Building ${data.type}")
        // TODO: Create Mesh here
    }
}