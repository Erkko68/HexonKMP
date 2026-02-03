package eric.bitria.hexon.threejs.engine

import eric.bitria.hexon.threejs.view.GameView
import kotlinx.browser.window

class Engine {
    val THREE = js("require('three')")

    // The Engine needs to talk to the View to update visuals
    var view: GameView? = null

    init {
        // Infrastructure: Expose THREE globally so View can use it safely
        window.asDynamic().THREE = THREE
        console.log("Engine initialized (Logic Only)")
    }

    // LOGIC & MESSAGE HANDLING
    fun handleCommand(command: String, data: dynamic) {
        // 1. Process Logic / Update State (TODO: Add Board State here)

        // 2. Trigger Visuals
        when (command) {
            "SetHex" -> view?.renderHex(data)
            "PlaceBuilding" -> view?.placeBuilding(data)
            else -> console.warn("Command $command not handled in Engine")
        }
    }

    fun start() {
        view = GameView(this)
        console.log("Engine logic started...")
    }
}