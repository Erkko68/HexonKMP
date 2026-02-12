package eric.bitria.hexon.threejs.loader

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
        val sqrt3 = sqrt(3.0)
        val x = hexSize.toDouble() * (3.0 / 2.0 * q)
        val z = hexSize.toDouble() * (sqrt3 / 2.0 * q + sqrt3 * r)
        val y = 0f

        model.position.set(x, y, z)
    }

    /**
     * Position a single digit of a number on top of a hexagon.
     * For multi-digit numbers, digits are positioned side by side.
     *
     * @param model The digit model to position
     * @param q Hex Q coordinate
     * @param r Hex R coordinate
     * @param hexSize Size of the hexagon
     * @param digitIndex Index of this digit (0 for first digit, 1 for second, etc.)
     * @param totalDigits Total number of digits in the number
     * @param heightOffset Height above the hexagon surface (default 0.5)
     * @param digitSpacing Horizontal spacing between digits (default 0.6)
     */
    fun positionNumberDigit(
        model: dynamic,
        q: Int,
        r: Int,
        hexSize: Float = 1f,
        digitIndex: Int = 0,
        totalDigits: Int = 1,
        heightOffset: Float = 0.01f,
        digitSpacing: Float = 0.8f
    ) {
        val sqrt3 = sqrt(3.0)

        // Calculate base hex position
        val baseX = hexSize.toDouble() * (3.0 / 2.0 * q)
        val baseZ = hexSize.toDouble() * (sqrt3 / 2.0 * q + sqrt3 * r)

        // Calculate horizontal offset for this digit
        // Center the entire number, then offset each digit
        val totalWidth = (totalDigits - 1) * digitSpacing
        val offsetLocal = (digitIndex * digitSpacing) - (totalWidth / 2.0)

        // Rotate the offset by 120 degrees (2.0944 radians) to match model rotation
        val rotationRadians = 1.0472 // 120 degrees in radians
        val offsetX = offsetLocal * cos(rotationRadians)
        val offsetZ = -offsetLocal * sin(rotationRadians)

        val x = baseX + offsetX
        val z = baseZ + offsetZ
        val y = heightOffset

        model.position.set(x, y, z)
        model.rotation.set(0, rotationRadians, 0) // Use radians, not degrees
    }

    /**
     * Position a vertex building in 3D space.
     * Takes three hex coordinates that define the vertex.
     */
    fun positionVertexBuilding(
        model: dynamic,
        hexA: Pair<Int, Int>,
        hexB: Pair<Int, Int>,
        hexC: Pair<Int, Int>,
        hexSize: Float = 1f,
    ) {
        // Calculate center point of the three hex coordinates
        val sqrt3 = sqrt(3.0)

        val hexCoords = listOf(hexA, hexB, hexC)
        var totalX = 0.0
        var totalZ = 0.0

        hexCoords.forEach { (q, r) ->
            val x = hexSize.toDouble() * (3.0 / 2.0 * q)
            val z = hexSize.toDouble() * (sqrt3 / 2.0 * q + sqrt3 * r)
            totalX += x
            totalZ += z
        }

        val avgX = totalX / hexCoords.size
        val avgZ = totalZ / hexCoords.size
        val y = 0.0

        model.position.set(avgX, y, avgZ)
    }

    /**
     * Position an edge building in 3D space.
     * Takes two hex coordinates that define the edge, and rotates the building
     * to align with the edge between the two hexagons.
     */
    fun positionEdgeBuilding(
        model: dynamic,
        hexA: Pair<Int, Int>,
        hexB: Pair<Int, Int>,
        hexSize: Float = 1f,
    ) {
        // Calculate positions of both hexagons
        val sqrt3 = sqrt(3.0)

        val (qA, rA) = hexA
        val (qB, rB) = hexB

        val xA = hexSize.toDouble() * (3.0 / 2.0 * qA)
        val zA = hexSize.toDouble() * (sqrt3 / 2.0 * qA + sqrt3 * rA)

        val xB = hexSize.toDouble() * (3.0 / 2.0 * qB)
        val zB = hexSize.toDouble() * (sqrt3 / 2.0 * qB + sqrt3 * rB)

        // Calculate midpoint (center of the edge)
        val avgX = (xA + xB) / 2.0
        val avgZ = (zA + zB) / 2.0
        val y = 0.01

        // Normalize edge direction: calculate the difference vector
        val dq = qB - qA
        val dr = rB - rA

        val adjustment = kotlin.math.PI / 3.0

        // For flat-top hex, determine the edge angle based on the neighbor direction
        // The 6 possible edge directions in axial coordinates are:
        // (+1, 0), (+1, -1), (0, -1), (-1, 0), (-1, +1), (0, +1)
        val angle = when {
            dq == 1 && dr == 0   -> 0.0                    // East edge
            dq == 1 && dr == -1  -> kotlin.math.PI / 3.0   // Northeast edge
            dq == 0 && dr == -1  -> 2.0 * kotlin.math.PI / 3.0  // Northwest edge
            dq == -1 && dr == 0  -> kotlin.math.PI         // West edge
            dq == -1 && dr == 1  -> 4.0 * kotlin.math.PI / 3.0  // Southwest edge
            dq == 0 && dr == 1   -> 5.0 * kotlin.math.PI / 3.0  // Southeast edge
            // Handle reverse directions (should be same as forward but we normalize)
            dq == -1 && dr == 0   -> 0.0
            dq == -1 && dr == 1   -> kotlin.math.PI / 3.0
            dq == 0 && dr == 1    -> 2.0 * kotlin.math.PI / 3.0
            dq == 1 && dr == 0    -> kotlin.math.PI
            dq == 1 && dr == -1   -> 4.0 * kotlin.math.PI / 3.0
            dq == 0 && dr == -1   -> 5.0 * kotlin.math.PI / 3.0
            else -> kotlin.math.atan2(zB - zA, xB - xA) + (kotlin.math.PI / 2.0)
        }

        model.position.set(avgX, y, avgZ)

        // Apply rotation around Y-axis to align with the edge
        model.rotation.y = angle + adjustment
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
    
}
