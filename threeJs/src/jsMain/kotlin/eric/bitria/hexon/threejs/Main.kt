package eric.bitria.hexon.threejs

import eric.bitria.hexon.threejs.engine.GameController
import eric.bitria.hexon.threejs.engine.Renderer
import eric.bitria.hexon.threejs.view.GameView
import kotlinx.browser.window

fun main() {

    // Expose THREE
    val THREE = js("require('three')")
    window.asDynamic().THREE = THREE

    // GLTFLoader is now imported from the 'three' module directly
    val GLTFLoader = js("require('three').GLTFLoader")
    window.asDynamic().GLTFLoader = GLTFLoader

    val renderer = Renderer()
    val gameController = GameController()
    val view = GameView(gameController, renderer)

    gameController.view = view
}
