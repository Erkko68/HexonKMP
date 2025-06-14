import * as THREE from 'three';

/**
 * InputHandler handles camera interactions via touch and wheel inputs.
 * It supports:
 * - Single-finger pan
 * - Two-finger pinch-to-zoom and pan
 * - Wheel zooming
 * - Double-tap to reset the camera to its initial position and zoom
 *
 * This class is designed for use with an orthographic camera in a 3D scene.
 */
const STATE = {
    NONE: -1,
    TOUCH_PAN: 0,
    TOUCH_ZOOM_PAN: 1
};

export class InputHandler {
    /**
     * Initializes the input handler.
     * @param {Object} sceneManager - An object containing `camera` and `renderer.domElement`.
     */
    constructor(sceneManager) {
        this.camera = sceneManager.camera;
        this.dom = sceneManager.renderer.domElement;
        this.enabled = true;

        // Interaction state
        this._state = STATE.NONE;

        // For touch zoom
        this._touchZoomDistanceStart = 0;
        this._touchZoomDistanceEnd = 0;

        // For touch pan
        this._panStart = new THREE.Vector2();
        this._panEnd = new THREE.Vector2();

        // For double-tap detection
        this._lastTapTime = 0;
        this._doubleTapThreshold = 300; // milliseconds

        // Store default camera position and zoom for reset
        this._defaultCameraPosition = this.camera.position.clone();
        this._defaultZoom = this.camera.zoom;

        // Set up event listeners
        this.dom.addEventListener('touchstart', this.touchstart.bind(this), {passive: false});
        this.dom.addEventListener('touchmove', this.touchmove.bind(this), {passive: false});
        this.dom.addEventListener('touchend', this.touchend.bind(this));
        this.dom.addEventListener('wheel', e => {
            e.preventDefault();
            this.zoomCamera(e.deltaY * 0.005);
        }, {passive: false});
    }

    /**
     * Called when a touch starts. Handles pan, zoom, and double-tap detection.
     */
    touchstart(event) {
        if (!this.enabled) return;

        const currentTime = performance.now();

        // Detect double tap (within threshold and using one finger)
        if (event.touches.length === 1 && currentTime - this._lastTapTime < this._doubleTapThreshold) {
            this.resetCamera();
            this._lastTapTime = 0; // Avoid triple tap triggering again
        } else {
            this._lastTapTime = currentTime;
        }

        switch (event.touches.length) {
            case 1:
                this._state = STATE.TOUCH_PAN;
                this._panStart.set(event.touches[0].pageX, event.touches[0].pageY);
                this._panEnd.copy(this._panStart);
                break;

            case 2:
                this._state = STATE.TOUCH_ZOOM_PAN;

                // Calculate initial zoom distance
                const dx = event.touches[0].pageX - event.touches[1].pageX;
                const dy = event.touches[0].pageY - event.touches[1].pageY;
                this._touchZoomDistanceStart = this._touchZoomDistanceEnd = Math.sqrt(dx * dx + dy * dy);

                // Midpoint of the two fingers for pan
                const x = (event.touches[0].pageX + event.touches[1].pageX) / 2;
                const y = (event.touches[0].pageY + event.touches[1].pageY) / 2;
                this._panStart.set(x, y);
                this._panEnd.copy(this._panStart);
                break;

            default:
                this._state = STATE.NONE;
        }
    }

    /**
     * Called when a touch moves. Handles camera panning and zooming.
     */
    touchmove(event) {
        if (!this.enabled) return;

        event.preventDefault();
        event.stopPropagation();

        switch (event.touches.length) {
            case 1:
                if (this._state === STATE.TOUCH_PAN) {
                    this._panEnd.set(event.touches[0].pageX, event.touches[0].pageY);
                    this.panCamera();
                    this._panStart.copy(this._panEnd);
                }
                break;

            case 2:
                // Update zoom distance
                const dx = event.touches[0].pageX - event.touches[1].pageX;
                const dy = event.touches[0].pageY - event.touches[1].pageY;
                this._touchZoomDistanceEnd = Math.sqrt(dx * dx + dy * dy);

                // Update pan midpoint
                const x = (event.touches[0].pageX + event.touches[1].pageX) / 2;
                const y = (event.touches[0].pageY + event.touches[1].pageY) / 2;
                this._panEnd.set(x, y);

                // Zoom and pan camera
                this.zoomCamera((this._touchZoomDistanceStart - this._touchZoomDistanceEnd) * 0.005);
                this._touchZoomDistanceStart = this._touchZoomDistanceEnd;

                this.panCamera();
                this._panStart.copy(this._panEnd);
                break;

            default:
                this._state = STATE.NONE;
        }
    }

    /**
     * Called when a touch ends. Resets interaction state as needed.
     */
    touchend(event) {
        if (!this.enabled) return;

        // Reset zoom distance tracking if fewer than 2 fingers
        if (event.touches.length < 2) {
            this._touchZoomDistanceStart = this._touchZoomDistanceEnd = 0;
        }

        if (event.touches.length === 1) {
            this._panStart.set(event.touches[0].pageX, event.touches[0].pageY);
            this._panEnd.copy(this._panStart);
            this._state = STATE.TOUCH_PAN;
        } else {
            this._state = STATE.NONE;
        }
    }

    /**
     * Adjusts the camera zoom by a delta value.
     * @param {number} deltaZoom - The zoom delta factor.
     */
    zoomCamera(deltaZoom) {
        this.camera.zoom = THREE.MathUtils.clamp(this.camera.zoom * (1 - deltaZoom), 0.5, 5);
        this.camera.updateProjectionMatrix();
    }

    /**
     * Pans the camera based on touch movement.
     */
    panCamera() {
        const deltaX = this._panEnd.x - this._panStart.x;
        const deltaY = this._panEnd.y - this._panStart.y;
        const width = this.dom.clientWidth;
        const height = this.dom.clientHeight;

        // Convert screen space movement to world space
        let moveX = (deltaX / width) * (this.camera.right - this.camera.left);
        let moveY = (deltaY / height) * (this.camera.top - this.camera.bottom);

        // Scale with inverse zoom for consistent movement
        const zoomFactor = 1.0 / this.camera.zoom;
        moveX *= zoomFactor;
        moveY *= zoomFactor;

        // Pan along isometric vectors
        const rightVector = new THREE.Vector3(1, 0, -1).normalize();  // Horizontal
        const upVector = new THREE.Vector3(-1, 0, -1).normalize();    // Vertical

        const panVector = new THREE.Vector3()
            .addScaledVector(rightVector, -moveX)
            .addScaledVector(upVector, moveY);

        this.camera.position.add(panVector);
        this.camera.updateMatrixWorld();
    }

    /**
     * Resets the camera to its initial position and zoom level.
     */
    resetCamera() {
        this.camera.position.copy(this._defaultCameraPosition);
        this.camera.zoom = this._defaultZoom;
        this.camera.updateProjectionMatrix();
        this.camera.updateMatrixWorld();
    }
}
