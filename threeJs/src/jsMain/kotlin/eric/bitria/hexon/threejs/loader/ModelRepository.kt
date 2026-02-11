package eric.bitria.hexon.threejs.loader

import kotlin.js.Promise


/**
 * Repository for managing GLB model loading and caching.
 * Acts as a single source of truth for model management across the application.
 */
class ModelRepository {

    // In-memory cache of loaded models
    private val modelCache = mutableMapOf<String, dynamic>()

    // Track pending requests to avoid duplicate loads
    private val loadingPromises = mutableMapOf<String, Promise<dynamic>>()

    /**
     * Fetch a hex tile model by resource ID.
     * Returns a Promise that resolves to a cloned model.
     */
    fun getHexModel(resourceId: String): Promise<dynamic> {
        return getModel(ModelPaths.hex(resourceId), resourceId)
    }

    /**
     * Fetch a building model by building ID.
     * Returns a Promise that resolves to a cloned model.
     */
    fun getBuildingModel(buildingId: String): Promise<dynamic> {
        return getModel(ModelPaths.building(buildingId), buildingId)
    }

    /**
     * Generic model fetching with caching.
     * Always returns a Promise that resolves to a cloned model.
     * If the model is cached, returns an immediately resolved Promise.
     * If currently loading, chains off the existing Promise to return a clone.
     * Otherwise, initiates a new load.
     */
    private fun getModel(modelPath: String, modelId: String): Promise<dynamic> {
        // Check if already cached - return a resolved Promise with a clone
        if (modelCache.containsKey(modelId)) {
            return Promise.resolve(cloneModel(modelCache[modelId]!!))
        }

        // Check if currently loading - chain to return a fresh clone
        if (loadingPromises.containsKey(modelId)) {
            return loadingPromises[modelId]!!.then { _: dynamic ->
                cloneModel(modelCache[modelId]!!)
            }
        }

        // Initiate new load
        val promise = loadModelFromPath(modelPath)
            .then { model: dynamic ->
                // Cache the original model
                modelCache[modelId] = model
                // Return a clone for this request
                cloneModel(model)
            }
            .finally {
                // Remove from loading promises once done
                loadingPromises.remove(modelId)
            }

        loadingPromises[modelId] = promise
        return promise
    }

    /**
     * Load a GLB model from a URL using Three.js GLTFLoader
     */
    private fun loadModelFromPath(modelPath: String): Promise<dynamic> {
        return Promise { resolve: (dynamic) -> Unit, reject: (Throwable) -> Unit ->
            val loader = js("new GLTFLoader()")
            loader.load(
                modelPath,
                { gltf: dynamic ->
                    console.log("Loaded model: $modelPath")
                    resolve(gltf.scene)
                },
                { _: dynamic -> },  // onProgress
                { error: dynamic ->
                    console.error("Failed to load model: $modelPath", error)
                    reject(error)
                }
            )
        }
    }

    /**
     * Clone a Three.js Object3D to create independent instances
     */
    @Suppress("UNUSED_PARAMETER", "UNUSED")
    private fun cloneModel(model: dynamic): dynamic {
        return js("model.clone(true)")
    }

    /**
     * Clear the model cache (useful for memory management)
     */
    @Suppress("UNUSED")
    fun clearCache() {
        modelCache.clear()
        loadingPromises.clear()
    }
}

