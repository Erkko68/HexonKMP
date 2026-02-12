package eric.bitria.hexon.threejs.view

import eric.bitria.hexon.render.GameCommand
import eric.bitria.hexon.threejs.engine.GameController
import eric.bitria.hexon.threejs.engine.Renderer
import eric.bitria.hexon.threejs.loader.ModelLoader
import eric.bitria.hexon.threejs.loader.ModelRepository

class GameView(
    val gameController: GameController,
    private val renderer: Renderer
) {
    private val modelRepository = ModelRepository()

    // Cache hex size from the first loaded model
    private var hexSize: Float = 10f // Default fallback

    init {
        // Link Engine to View
        gameController.view = this
    }

    // VISUAL API (Called by Engine)
    fun renderHex(data: GameCommand.SetHex) {
        console.log("View: Drawing Hex at ${data.coord.q}, ${data.coord.r}, resource: ${data.resource}")

        // Fetch the hex model for this resource (wood, ore, sheep, etc.)
        val hexPromise = modelRepository.getHexModel(data.resource)

        hexPromise.then { model: dynamic ->
            // Update hex size from model dimensions
            hexSize = ModelLoader.getHexSizeFromModel(model).toFloat()

            // Position the hex using cube coordinates
            ModelLoader.positionHex(model, data.coord.q, data.coord.r, hexSize)

            // Add the model to the scene
            renderer.scene.add(model)

            console.log("Rendered hex: ${data.resource} at (${data.coord.q}, ${data.coord.r})")

            // Render number digits after hex is positioned
            renderNumberOnHex(data.number, data.coord.q, data.coord.r)
        }.catch { error: dynamic ->
            console.error("Failed to render hex: ${data.resource}", error)
        }
    }

    /**
     * Render number on top of a hexagon.
     * Multi-digit numbers are split into individual digit models and placed side by side.
     */
    private fun renderNumberOnHex(number: Int, q: Int, r: Int) {
        val numberString = number.toString()
        val digits = numberString.map { it.toString() }

        console.log("Rendering number $number as ${digits.size} digit(s) at ($q, $r)")

        // Load and position each digit
        digits.forEachIndexed { index, digit ->
            val digitPromise = modelRepository.getNumberModel(digit)

            digitPromise.then { model: dynamic ->
                // Position each digit side by side
                ModelLoader.positionNumberDigit(
                    model = model,
                    q = q,
                    r = r,
                    hexSize = hexSize,
                    digitIndex = index,
                    totalDigits = digits.size
                )

                // Add the digit model to the scene
                renderer.scene.add(model)

                console.log("Rendered digit '$digit' (${index + 1}/${digits.size}) at ($q, $r)")
            }.catch { error: dynamic ->
                console.error("Failed to render digit: $digit", error)
            }
        }
    }

    fun placeBuilding(data: GameCommand.PlaceBuilding) {
        console.log("View: Placing Building ${data.buildingId} for player ${data.player}")

        // Fetch the building model
        val modelPromise = modelRepository.getBuildingModel(data.buildingId)

        modelPromise.then { model: dynamic ->
            // Collect hex coordinates (vertex or edge placement)
            val hexCoords = listOf(
                data.hexA.q to data.hexA.r,
                data.hexB.q to data.hexB.r
            ) + if (data.hexC != null) {
                listOf(data.hexC!!.q to data.hexC!!.r)
            } else {
                emptyList()
            }

            // Position the building using captured hex size
            ModelLoader.positionBuilding(model, hexCoords, hexSize)

            // Add the model to the scene
            renderer.scene.add(model)

            console.log("Placed building: ${data.buildingId} for player ${data.player}")
        }.catch { error: dynamic ->
            console.error("Failed to place building: ${data.buildingId}", error)
        }
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