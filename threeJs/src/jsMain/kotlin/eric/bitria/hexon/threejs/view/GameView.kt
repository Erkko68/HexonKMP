package eric.bitria.hexon.threejs.view

import eric.bitria.hexon.render.GameCommand
import eric.bitria.hexon.threejs.engine.GameController
import eric.bitria.hexon.threejs.engine.Renderer

class GameView(
    val gameController: GameController,
    private val renderer: Renderer
) {
    init {
        // Link Engine to View
        gameController.view = this
    }

    // VISUAL API (Called by Engine)
    fun renderHex(data: GameCommand.SetHex) {
        console.log("View: Drawing Hex at ${data.coord.q}, ${data.coord.r}")
        // TODO: Create Mesh here and add it to renderer.scene
    }

    fun placeBuilding(data: GameCommand.PlaceBuilding) {
        console.log("View: Placing Building ${data.buildingId}")
        // TODO: Create Mesh here and add it to renderer.scene
    }

    fun setPort(cmd: GameCommand.SetPort) {
        TODO("Not yet implemented")
    }

    fun robberUpdated(cmd: GameCommand.RobberUpdated) {
        TODO("Not yet implemented")
    }

    fun showVertexBuildingPositions(cmd: GameCommand.ShowVertexBuildingPositions) {
        TODO("Not yet implemented")
    }

    fun showEdgeBuildingPositions(cmd: GameCommand.ShowEdgeBuildingPositions) {
        TODO("Not yet implemented")
    }

    fun diceRolled(cmd: GameCommand.DiceRolled) {
        TODO("Not yet implemented")
    }
}