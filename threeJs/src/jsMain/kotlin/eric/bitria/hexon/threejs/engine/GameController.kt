@file:OptIn(ExperimentalSerializationApi::class)

package eric.bitria.hexon.threejs.engine

import eric.bitria.hexon.threejs.AppBridge
import eric.bitria.hexon.threejs.command.CommandParser
import eric.bitria.hexon.threejs.view.GameView
import kotlinx.serialization.ExperimentalSerializationApi

class GameController {

    var view: GameView? = null

    init {
        if (js("typeof AppBridge !== 'undefined'") as Boolean) {
            AppBridge.on("SetHex") { data: dynamic -> setHex(data) }
            AppBridge.on("SetPort") { data: dynamic -> setPort(data) }
            AppBridge.on("RobberUpdated") { data: dynamic -> robberUpdated(data) }
            AppBridge.on("PlaceBuilding") { data: dynamic -> placeBuilding(data) }
            AppBridge.on("ShowVertexBuildingPositions") { data: dynamic -> showVertexBuildingPositions(data) }
            AppBridge.on("ShowEdgeBuildingPositions") { data: dynamic -> showEdgeBuildingPositions(data) }
            AppBridge.on("DiceRolled") { data: dynamic -> diceRolled(data) }

            AppBridge.call("onEngineReady", "ready")
        }
    }

    private fun setHex(data: dynamic) {
        val cmd = CommandParser.parseSetHex(data)
        view?.renderHex(cmd)
    }

    private fun placeBuilding(data: dynamic) {
        val cmd = CommandParser.parsePlaceBuilding(data)
        view?.placeBuilding(cmd)
    }

    private fun setPort(data: dynamic) {
        val cmd = CommandParser.parseSetPort(data)
        view?.setPort(cmd)
    }

    private fun robberUpdated(data: dynamic) {
        val cmd = CommandParser.parseRobberUpdated(data)
        view?.robberUpdated(cmd)
    }

    private fun showVertexBuildingPositions(data: dynamic) {
        val cmd = CommandParser.parseShowVertexBuildingPositions(data)
        view?.showVertexBuildingPositions(cmd)
    }

    private fun showEdgeBuildingPositions(data: dynamic) {
        val cmd = CommandParser.parseShowEdgeBuildingPositions(data)
        view?.showEdgeBuildingPositions(cmd)
    }

    private fun diceRolled(data: dynamic) {
        val cmd = CommandParser.parseDiceRolled(data)
        view?.diceRolled(cmd)
    }
}