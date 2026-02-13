package eric.bitria.hexon.threejs.view

import eric.bitria.hexon.game.data.def.PlacementType
import eric.bitria.hexon.render.GameCommand
import eric.bitria.hexon.threejs.AppBridge
import eric.bitria.hexon.threejs.engine.GameController
import eric.bitria.hexon.threejs.engine.Renderer
import eric.bitria.hexon.threejs.input.Raycaster
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

    // Raycaster for detecting clicks on ghost buildings
    private val raycaster = Raycaster(renderer)

    init {
        // Link Engine to View
        gameController.view = this

        // Setup click handler for ghost buildings
        setupGhostBuildingClickHandler()
    }

    private fun setupGhostBuildingClickHandler() {
        raycaster.onObjectClicked = { clickedObject ->
            // Check if the clicked object is a ghost building
            if (clickedObject.userData.isGhostBuilding == true) {
                handleGhostBuildingClick(clickedObject)
            }
            clearGhostBuildings()
        }
    }

    private fun handleGhostBuildingClick(ghostObject: dynamic) {
        val buildingId = ghostObject.userData.buildingId as? String
        val placementType = ghostObject.userData.placementType as? String
        val hexA = ghostObject.userData.hexA as? String
        val hexB = ghostObject.userData.hexB as? String
        val hexC = ghostObject.userData.hexC as? String

        if (buildingId == null || placementType == null || hexA == null || hexB == null) {
            console.error("Ghost building missing required metadata")
            return
        }

        console.log("Ghost building clicked: $buildingId at ($hexA, $hexB, $hexC)")

        // Create a plain JS object for the event
        // HexCoord values are sent as strings in "q,r" format (matching HexCoordKeySerializer)
        val eventData = js("{}")
        eventData.buildingId = buildingId
        eventData.type = placementType
        eventData.hexA = hexA  // e.g., "0,0"
        eventData.hexB = hexB  // e.g., "1,0"
        eventData.hexC = hexC  // e.g., "0,1" or null

        // Send event via AppBridge
        if (js("typeof AppBridge !== 'undefined'") as Boolean) {
            console.log("Sending GameEvent: ", eventData)
            AppBridge.call("GameEvent", eventData)
        } else {
            console.error("AppBridge not available")
        }
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
            // Apply player color to the model
            applyPlayerColor(model, data.color)

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

                // Store metadata in userData for raycasting
                model.userData.isGhostBuilding = true
                model.userData.buildingId = cmd.buildingId
                model.userData.placementType = "VERTEX"

                // Store coordinates as strings in "q,r" format (matching HexCoordKeySerializer)
                model.userData.hexA = "${hexA.q},${hexA.r}"
                model.userData.hexB = "${hexB.q},${hexB.r}"
                model.userData.hexC = "${hexC.q},${hexC.r}"


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

                // Store metadata in userData for raycasting
                model.userData.isGhostBuilding = true
                model.userData.buildingId = cmd.buildingId
                model.userData.placementType = "EDGE"

                // Store coordinates as strings in "q,r" format (matching HexCoordKeySerializer)
                model.userData.hexA = "${hexA.q},${hexA.r}"
                model.userData.hexB = "${hexB.q},${hexB.r}"
                model.userData.hexC = null

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

    /**
     * Apply player color to a model by finding the material named 'playerColor'
     * and setting its color to the provided hex color string (e.g., "#FF0000").
     */
    private fun applyPlayerColor(model: dynamic, colorHex: String) {
        model.traverse { child: dynamic ->
            if (child.isMesh == true && child.material != null) {
                // check if the material is an array
                val isArray = js("Array.isArray(child.material)") as Boolean

                if (isArray) {
                    // Handle material array
                    val materials = child.material as Array<dynamic> // Cast to array for access
                    val newMaterials = materials.map { material ->
                        if (material.name == "playerColor") {
                            val newMaterial = material.clone()
                            newMaterial.color.set(colorHex)
                            console.log("Applied player color $colorHex to cloned material")
                            newMaterial
                        } else {
                            material
                        }
                    }.toTypedArray()
                    child.material = newMaterials
                } else {
                    // Handle single material
                    val material = child.material
                    if (material.name == "playerColor") {
                        val newMaterial = material.clone()
                        newMaterial.color.set(colorHex)
                        child.material = newMaterial
                        console.log("Applied player color $colorHex to cloned material")
                    }
                }
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