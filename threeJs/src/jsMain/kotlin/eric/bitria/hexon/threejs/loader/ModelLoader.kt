package eric.bitria.hexon.threejs.loader

/**
 * Utilities for loading and positioning models in the Three.js scene.
 */
object ModelLoader {

    /**
     * Position a model in 3D space using hex coordinates.
     * Converts hex cube coordinates (q, r) to cartesian 3D space.
     *
     * For a hex grid with flat-top hexagons:
     * - x = size * (3/2 * q)
     * - z = size * (√3/2 * q + √3 * r)
     * - y = 0 (flat on ground)
     */
    fun positionHex(model: dynamic, q: Int, r: Int, hexSize: Float = 1f) {
        val sqrt3 = js("Math.sqrt(3)") as Double
        val x = hexSize.toDouble() * (3.0 / 2.0 * q)
        val z = hexSize.toDouble() * (sqrt3 / 2.0 * q + sqrt3 * r)
        val y = 0f

        model.position.set(x, y, z)
    }

    /**
     * Position a building (vertex or edge building) in 3D space.
     * Takes multiple hex coordinates to determine the position.
     */
    fun positionBuilding(
        model: dynamic,
        hexCoords: List<Pair<Int, Int>>,
        hexSize: Float = 1f,
        heightOffset: Float = 0.5f
    ) {
        // Calculate center point of the hex coordinates
        var totalX = 0.0
        var totalZ = 0.0
        val sqrt3 = js("Math.sqrt(3)") as Double

        hexCoords.forEach { (q, r) ->
            val x = hexSize.toDouble() * (3.0 / 2.0 * q)
            val z = hexSize.toDouble() * (sqrt3 / 2.0 * q + sqrt3 * r)
            totalX += x
            totalZ += z
        }

        val avgX = totalX / hexCoords.size
        val avgZ = totalZ / hexCoords.size
        val y = heightOffset

        model.position.set(avgX, y, avgZ)
    }

    /**
     * Scale a model uniformly
     */
    @Suppress("UNUSED")
    fun scale(model: dynamic, scale: Float) {
        model.scale.set(scale, scale, scale)
    }

    /**
     * Rotate a model on the Y axis (in radians)
     */
    @Suppress("UNUSED")
    fun rotateY(model: dynamic, radians: Float) {
        model.rotation.y = radians
    }

    /**
     * Add materials and textures properties to improve visual appearance
     */
    @Suppress("UNUSED")
    fun applyMaterial(model: dynamic, color: Int = 0xffffff, metalness: Float = 0.5f, roughness: Float = 0.5f) {
        // Traverse all children and apply material
        model.traverse { child: dynamic ->
            if (js("child.isMesh") as Boolean) {
                if (js("child.material") != null) {
                    child.material.color.setHex(color)
                    if (js("child.material.metalness !== undefined") as Boolean) {
                        child.material.metalness = metalness
                    }
                    if (js("child.material.roughness !== undefined") as Boolean) {
                        child.material.roughness = roughness
                    }
                }
            }
        }
    }

    /**
     * Compute the bounding box of a model.
     * Returns an object with min, max, and size vectors.
     */
    fun computeBoundingBox(model: dynamic): dynamic {
        val box = js("new THREE.Box3()")
        box.setFromObject(model)

        val size = js("new THREE.Vector3()")
        box.getSize(size)

        return js("({ min: box.min, max: box.max, size: size })")
    }

    /**
     * Get the hex size from a model's bounding box.
     * For flat-top hexagons, the width (x-axis) is 2 * size,
     * so we return width / 2.
     */
    fun getHexSizeFromModel(model: dynamic): Double {
        val bounds = computeBoundingBox(model)
        val width = bounds.size.x as Double
        // For flat-top hex: width = 2 * hexSize, so hexSize = width / 2
        return width / 2.0
    }

    /**
     * Get the full dimensions of a model.
     * Returns a Triple of (width, height, depth).
     */
    fun getModelDimensions(model: dynamic): Triple<Double, Double, Double> {
        val bounds = computeBoundingBox(model)
        return Triple(
            bounds.size.x as Double,
            bounds.size.y as Double,
            bounds.size.z as Double
        )
    }
}
