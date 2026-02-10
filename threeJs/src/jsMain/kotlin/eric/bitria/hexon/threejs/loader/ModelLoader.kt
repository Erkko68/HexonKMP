package eric.bitria.hexon.threejs.loader

import kotlin.js.Promise

/**
 * Handles loading and caching of GLB/GLTF 3D models.
 * Uses the global window.GLTFLoader which is created once in Main.kt to avoid duplicate instances.
 */
class ModelLoader() {

    // Use global GLTFLoader exposed on window by Main.kt
    private val loader: dynamic = js("window.GLTFLoader")

    // Cache for loaded models (URL -> loaded GLTF scene)
    private val cache = mutableMapOf<String, dynamic>()

    // Pending loads to prevent duplicate requests
    private val pending = mutableMapOf<String, Promise<dynamic>>()

    /**
     * Load a building model by its ID (e.g., "road", "settlement").
     */
    fun loadBuilding(buildingId: String): Promise<dynamic> {
        return load(ModelPaths.building(buildingId))
    }

    /**
     * Load a hex tile model by resource ID (e.g., "wood", "ore").
     */
    fun loadHex(resourceId: String): Promise<dynamic> {
        return load(ModelPaths.hex(resourceId))
    }

    /**
     * Load a GLB model from the given URL.
     * Returns a cloned scene to allow multiple instances.
     */
    fun load(url: String): Promise<dynamic> {
        // Return cached model (cloned)
        val cached = cache[url]
        if (cached != null) {
            return Promise.resolve(cloneModel(cached))
        }

        // Return pending request if one exists
        val pendingPromise = pending[url]
        if (pendingPromise != null) {
            return pendingPromise.then<dynamic> { cached -> cloneModel(cached) }
        }

        // Create new load request
        val promise = Promise<dynamic> { resolve, reject ->
            loader.load(
                url,
                { gltf: dynamic ->
                    cache[url] = gltf.scene
                    pending.remove(url)
                    resolve(cloneModel(gltf.scene))
                },
                { _: dynamic -> /* progress */ },
                { error: dynamic ->
                    pending.remove(url)
                    console.error("ModelLoader: Failed to load $url", error)
                    reject(error)
                }
            )
        }

        pending[url] = promise
        return promise
    }

    /**
     * Clear all cached models.
     */
    fun clearCache() {
        cache.clear()
    }

    private fun cloneModel(scene: dynamic): dynamic {
        return scene.clone()
    }
}
