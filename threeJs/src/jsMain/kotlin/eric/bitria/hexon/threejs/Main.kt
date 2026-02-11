package eric.bitria.hexon.threejs

import eric.bitria.hexon.threejs.engine.GameController
import eric.bitria.hexon.threejs.engine.Renderer
import eric.bitria.hexon.threejs.view.GameView
import kotlinx.browser.window

fun main() {
    // 1. Load Core Three.js
    val THREE = js("require('three')")
    window.asDynamic().THREE = THREE

    // 2. Load GLTFLoader from three-stdlib
    val GLTFLoader = js("require('three/examples/jsm/loaders/GLTFLoader.js').GLTFLoader")
    window.asDynamic().GLTFLoader = GLTFLoader

    val renderer = Renderer()
    val gameController = GameController()
    val view = GameView(gameController, renderer)

    gameController.view = view
}