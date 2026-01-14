package eric.bitria.hexon.threejs

import kotlinx.browser.document
import kotlinx.browser.window

fun main() {
    initThreeJs()
}

fun initThreeJs() {
    println("ThreeJs Renderer Initializing...")

    // 1. Load Three.js
    val THREE = js("require('three')")
    if (THREE == undefined) {
        console.error("THREE.js not found! Webpack bundle might be missing.")
        return
    }

    // 2. Attach to Canvas
    val canvas = document.getElementById("three-root")
    if (canvas == null) {
        console.error("Canvas #three-root not found!")
        return
    }

    // 3. Setup Scene
    val scene = js("new THREE.Scene()")
    scene.background = js("new THREE.Color(0x333333)")

    val camera = js("new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000)")
    camera.position.z = 5

    val rendererConfig = js("{}")
    rendererConfig.canvas = canvas
    rendererConfig.antialias = true
    rendererConfig.alpha = true

    val renderer = js("new THREE.WebGLRenderer(rendererConfig)")
    renderer.setSize(window.innerWidth, window.innerHeight)

    // 4. Initial Scene Objects (Placeholder)
    val geometry = js("new THREE.BoxGeometry()")
    val material = js("new THREE.MeshBasicMaterial({ color: 0x00ff00 })")
    val cube = js("new THREE.Mesh(geometry, material)")
    scene.add(cube)

    var rotationSpeed = 0.01

    // 5. Animation Loop
    fun animate() {
        window.requestAnimationFrame { animate() }
        cube.rotation.x = (cube.rotation.x as Double) + rotationSpeed
        cube.rotation.y = (cube.rotation.y as Double) + rotationSpeed
        renderer.render(scene, camera)
    }
    animate()

    console.log("ThreeJS Initialized & Running")

    // 6. Bridge Integration
    if (js("typeof AppBridge !== 'undefined'") as Boolean) {

        // --- A. Handle Commands (Kotlin -> JS) ---
        AppBridge.on("updateSpeed") { data ->
            // data matches GameCommand.UpdateSpeed structure
            val newSpeed = data.speed as? Double ?: 0.01
            rotationSpeed = newSpeed
        }

        AppBridge.on("moveCamera") { data ->
            val x = data.x as Double
            val y = data.y as Double
            camera.position.x = x
            camera.position.y = y
        }

        // --- B. Send Events (JS -> Kotlin) ---

        // 1. Notify Ready
        AppBridge.call("onEngineReady", "ready")

    } else {
        console.warn("AppBridge not found.")
    }

    // 7. Handle Resize
    window.addEventListener("resize", {
        val width = window.innerWidth
        val height = window.innerHeight
        camera.aspect = width.toDouble() / height
        camera.updateProjectionMatrix()
        renderer.setSize(width, height)
    })
}