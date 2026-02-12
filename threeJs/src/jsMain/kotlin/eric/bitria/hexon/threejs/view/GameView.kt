package eric.bitria.hexon.threejs.view

import eric.bitria.hexon.game.data.def.PlacementType
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

    // Track ghost buildings for clearing
    private val ghostBuildings = mutableListOf<dynamic>()

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
        console.log("View: Placing ${data.placementType} Building ${data.buildingId} for player ${data.player}")

        // Fetch the building model
        val modelPromise = modelRepository.getBuildingModel(data.buildingId)

        modelPromise.then { model: dynamic ->
            when (data.placementType) {
                PlacementType.VERTEX -> {
                    // Vertex buildings require 3 coordinates
                    val hexC = data.hexC ?: run {
                        console.error("Vertex building requires hexC coordinate")
                        return@then
                    }
                    ModelLoader.positionVertexBuilding(
                        model,
                        data.hexA.q to data.hexA.r,
                        data.hexB.q to data.hexB.r,
                        hexC.q to hexC.r,
                        hexSize
                    )
                }
                PlacementType.EDGE -> {
                    // Edge buildings use 2 coordinates
                    ModelLoader.positionEdgeBuilding(
                        model,
                        data.hexA.q to data.hexA.r,
                        data.hexB.q to data.hexB.r,
                        hexSize
                    )
                }
            }

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
        console.log("View: Showing ${cmd.availablePositions.size} vertex positions for building ${cmd.buildingId}")

        // Clear any existing ghost buildings
        clearGhostBuildings()

        // Create ghost buildings at each available position
        cmd.availablePositions.forEach { (hexA, hexB, hexC) ->
            val modelPromise = modelRepository.getBuildingModel(cmd.buildingId)

            modelPromise.then { model: dynamic ->
                // Apply ghost material (semi-transparent white)
                applyGhostMaterial(model)

                // Position the building at the vertex
                ModelLoader.positionVertexBuilding(
                    model,
                    hexA.q to hexA.r,
                    hexB.q to hexB.r,
                    hexC.q to hexC.r,
                    hexSize
                )

                // Add to scene and track for later removal
                renderer.scene.add(model)
                ghostBuildings.add(model)

                console.log("Placed ghost building at vertex (${hexA}, ${hexB}, ${hexC})")
            }.catch { error: dynamic ->
                console.error("Failed to place ghost building: ${cmd.buildingId}", error)
            }
        }
    }

    /**
     * Apply a semi-transparent white material to a model to create a "ghost" effect.
     */
    private fun applyGhostMaterial(model: dynamic) {
        // 1. Create the material options as a JS object
        val materialParams = js("{}")
        materialParams.color = 0xffffff
        materialParams.transparent = true
        materialParams.opacity = 0.5
        materialParams.depthWrite = false

        // 2. Instantiate the material
        // We use a small js() call just to invoke the 'new' constructor,
        // passing our Kotlin-defined params object.
        val ghostMaterial = js("new THREE.MeshStandardMaterial(materialParams)")

        // 3. Traverse using a Kotlin lambda
        model.traverse { child: dynamic ->
            // Check if the child is a Mesh.
            // 'child.isMesh' is a boolean property in Three.js
            if (child.isMesh == true) {
                child.material = ghostMaterial
            }
        }
    }

    fun showEdgeBuildingPositions(cmd: GameCommand.ShowEdgeBuildingPositions) {
        console.log("View: Showing ${cmd.availablePositions.size} edge positions for building ${cmd.buildingId}")

        // Clear any existing ghost buildings
        clearGhostBuildings()

        // Create ghost buildings at each available position
        cmd.availablePositions.forEach { (hexA, hexB) ->
            val modelPromise = modelRepository.getBuildingModel(cmd.buildingId)

            modelPromise.then { model: dynamic ->
                // Apply ghost material (semi-transparent white)
                applyGhostMaterial(model)

                // Position the building at the edge (with rotation)
                ModelLoader.positionEdgeBuilding(
                    model,
                    hexA.q to hexA.r,
                    hexB.q to hexB.r,
                    hexSize
                )

                // Add to scene and track for later removal
                renderer.scene.add(model)
                ghostBuildings.add(model)

                console.log("Placed ghost building at edge (${hexA}, ${hexB})")
            }.catch { error: dynamic ->
                console.error("Failed to place ghost building: ${cmd.buildingId}", error)
            }
        }
    }

    fun diceRolled(cmd: GameCommand.DiceRolled) {
        TODO("Not yet implemented")
    }

    /**
     * Clear all ghost buildings from the scene.
     */
    private fun clearGhostBuildings() {
        ghostBuildings.forEach { ghostModel ->
            renderer.scene.remove(ghostModel)
        }
        ghostBuildings.clear()
        console.log("Cleared ghost buildings")
    }
}