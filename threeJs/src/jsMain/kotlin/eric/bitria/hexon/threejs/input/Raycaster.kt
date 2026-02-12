package eric.bitria.hexon.threejs.input

import eric.bitria.hexon.threejs.engine.Renderer

/**
 * Handles raycasting for mouse/touch interactions with 3D objects.
 * Uses Three.js Raycaster to detect clicks on objects in the scene.
 */
class Raycaster(private val renderer: Renderer) {

    private val raycaster: dynamic = js("new THREE.Raycaster()")
    private val mouse: dynamic = js("new THREE.Vector2()")

    /**
     * Callback invoked when an object is clicked.
     * Receives the clicked object and its userData.
     */
    var onObjectClicked: ((dynamic) -> Unit)? = null

    init {
        setupEventListeners()
    }

    private fun setupEventListeners() {
        val canvas = renderer.canvas

        canvas.addEventListener("click", { event: dynamic ->
            handleClick(event)
        })
    }

    private fun handleClick(event: dynamic) {
        val canvas = renderer.canvas

        // Calculate mouse position in normalized device coordinates (-1 to +1)
        val rect = canvas.getBoundingClientRect()
        mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
        mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

        // Update the raycaster with the camera and mouse position
        raycaster.setFromCamera(mouse, renderer.camera)

        // Calculate objects intersecting the ray
        val intersects = raycaster.intersectObjects(renderer.scene.children, true)

        if (intersects.length > 0) {
            // Get the first intersected object
            val intersectedObject = intersects[0].`object`

            // Traverse up to find the root object (in case we hit a child mesh)
            var targetObject = intersectedObject
            while (targetObject.parent != null && targetObject.parent != renderer.scene) {
                targetObject = targetObject.parent
            }

            console.log("Clicked object:", targetObject)
            console.log("Object userData:", targetObject.userData)

            // Invoke the callback if set
            onObjectClicked?.invoke(targetObject)
        }
    }

    /**
     * Dispose of event listeners when no longer needed.
     */
    @Suppress("unused")
    fun dispose() {
        // Note: In a real implementation, we'd need to store the event listener
        // reference to properly remove it. For now, this is a placeholder.
        console.log("Raycaster disposed")
    }
}

