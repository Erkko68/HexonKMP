package eric.bitria.hexon.threejs.engine

import eric.bitria.hexon.threejs.view.CameraController
import kotlinx.browser.document
import kotlinx.browser.window

class Renderer {

    private val canvas = document.getElementById("three-root")
        ?: throw IllegalStateException("Canvas #three-root not found")

    // VISUAL STATE: Scene belongs here
    val scene: dynamic = js("new THREE.Scene()")

    private val renderer: dynamic = run {
        val config = js("{}")
        config.canvas = canvas
        config.antialias = true
        js("new THREE.WebGLRenderer(config)")
    }

    private val cameraController = CameraController()

    init {
        setupVisuals()
        setupRenderer()
        animate()
    }

    // PURE VISUAL SETUP
    private fun setupVisuals() {
        scene.background = js("new THREE.Color(0x222222)")

        // GridHelper (Visual)
        val gridHelper = js("new THREE.GridHelper(20, 20, 0x888888, 0x444444)")
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
}

