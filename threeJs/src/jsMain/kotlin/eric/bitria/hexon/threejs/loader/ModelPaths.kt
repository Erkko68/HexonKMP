package eric.bitria.hexon.threejs.loader

/**
 * Simple path resolver for dynamically fetching models by ID.
 * Building and resource IDs come from GameCommand (e.g., "road", "settlement", "wood", "ore").
 */
object ModelPaths {

    private const val BASE_URL = "http://10.0.2.2:8080/assets/models"

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
}

