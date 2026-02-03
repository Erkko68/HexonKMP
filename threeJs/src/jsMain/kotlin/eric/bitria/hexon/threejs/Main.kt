package eric.bitria.hexon.threejs

import eric.bitria.hexon.threejs.engine.Engine

fun main() {
    val engine = Engine()

    engine.start()

    if (js("typeof AppBridge !== 'undefined'") as Boolean) {

        AppBridge.on("SetHex") { data -> engine.handleCommand("SetHex", data) }
        AppBridge.on("SetPort") { data -> engine.handleCommand("SetPort", data) }
        AppBridge.on("RobberUpdated") { data -> engine.handleCommand("RobberUpdated", data) }
        AppBridge.on("PlaceBuilding") { data -> engine.handleCommand("PlaceBuilding", data) }
        AppBridge.on("ShowVertexBuildingPositions") { data -> engine.handleCommand("ShowVertexBuildingPositions", data) }
        AppBridge.on("ShowEdgeBuildingPositions") { data -> engine.handleCommand("ShowEdgeBuildingPositions", data) }
        AppBridge.on("DiceRolled") { data -> engine.handleCommand("DiceRolled", data) }

        AppBridge.call("onEngineReady", "ready")
    }
}
