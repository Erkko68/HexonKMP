package eric.bitria.hexon.threejs.loader

import eric.bitria.hexon.config.EnvConfig

/**
 * Simple path resolver for dynamically fetching models by ID.
 * Building and resource IDs come from GameCommand (e.g., "road", "settlement", "wood", "ore").
 */
object ModelPaths {

    private val BASE_URL = "${EnvConfig.BASE_URL}/assets/models"

    /**
     * Get model path for a building (e.g., "road" -> "/models/buildings/road.glb")
     */
    fun building(buildingId: String): String = "$BASE_URL/buildings/$buildingId.glb"

    /**
     * Get model path for a hex tile resource (e.g., "wood" -> "/models/hexagons/hex_wood.glb")
     */
    fun hex(resourceId: String): String = "$BASE_URL/hexagons/$resourceId.glb"

    /**
     * Get model path for misc items (robber, markers, etc.)
     */
    fun misc(itemId: String): String = "$BASE_URL/misc/$itemId.glb"

    /**
     * Get model path for number models (e.g., "1" -> "/models/numbers/1.glb")
     */
    fun number(number: String): String = "$BASE_URL/numbers/$number.glb"
}

