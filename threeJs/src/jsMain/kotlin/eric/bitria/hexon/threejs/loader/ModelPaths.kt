package eric.bitria.hexon.threejs.loader

/**
 * Simple path resolver for dynamically fetching models by ID.
 * Building and resource IDs come from GameCommand (e.g., "road", "settlement", "wood", "ore").
 */
object ModelPaths {

    private const val BASE_URL = "http://192.168.100.254:8080" + "/assets/models"

    /**
     * Get model path for a building (e.g., "road" -> "/models/road.glb")
     */
    fun building(buildingId: String): String = "$BASE_URL/buildings/$buildingId.glb"

    /**
     * Get model path for a hex tile resource (e.g., "wood" -> "/models/hex_wood.glb")
     */
    fun hex(resourceId: String): String = "$BASE_URL/hexagons/$resourceId.glb"

    /**
     * Get model path for misc items (robber, markers, etc.)
     */
    fun misc(name: String): String = "$BASE_URL/$name.glb"
}



