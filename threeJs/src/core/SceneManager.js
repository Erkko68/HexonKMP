import * as THREE from 'three';
import { BoardManager } from './BoardManager.js';
import { InputHandler } from './InputHandler.js';
import Stats from 'stats.js';

/**
 * SceneManager handles Three.js scene setup, rendering, camera, lights,
 * event handling, input, and performance monitoring (FPS).
 */
export class SceneManager {
    constructor(container = document.body) {
        this.container = container;
        this.scene = new THREE.Scene();

        this.setupLights();           // Set up ambient and directional lighting
        this.setupCamera();           // Set up orthographic camera
        this.setupRenderer();         // Create WebGL renderer and attach to DOM

        this.boardManager = new BoardManager(this.scene); // Handle board generation
        this.inputHandler = new InputHandler(this);       // Handle user input

        // DEBUG
        this.visualizeGrid();         // Optional: add grid and axes helpers for debugging
        this.setupStats();            // Add FPS counter

        this.animate();               // Start animation loop
    }

    // RENDERING

    /**
     * Sets up an orthographic camera for an isometric perspective.
     */
    setupCamera() {
        const aspect = window.innerWidth / window.innerHeight;
        const d = 20;

        this.camera = new THREE.OrthographicCamera(
            -d * aspect, d * aspect,
            d, -d,
            1, 1000
        );

        this.camera.position.set(20, 20, 20); // Position for isometric view
        this.camera.lookAt(0, 0, 0);
    }

    /**
     * Adds basic ambient and directional lighting to the scene.
     */
    setupLights() {
        const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
        this.scene.add(ambientLight);

        const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
        directionalLight.position.set(1, 1, 0.5).normalize();
        this.scene.add(directionalLight);
    }

    /**
     * Initializes the WebGL renderer and appends it to the container.
     */
    setupRenderer() {
        this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
        this.renderer.setSize(window.innerWidth, window.innerHeight);
        this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        this.container.appendChild(this.renderer.domElement);
    }

    /**
     * Main render loop using requestAnimationFrame. Also updates FPS stats.
     */
    animate() {
        this.statsFPS.begin();
        this.statsMS.begin();
        this.statsMem.begin();

        requestAnimationFrame(() => this.animate());
        this.renderer.render(this.scene, this.camera);

        this.statsFPS.end();
        this.statsMS.end();
        this.statsMem.end();
    }

    // GAME RENDER

    /**
     * Public method to delegate board generation to the BoardManager.
     * @param {Object} boardConfig - Configuration object for board layout
     */
    generateBoard(boardConfig) {
        this.boardManager.generateBoard(boardConfig);
    }

    // DEBUG

    /**
     * Adds FPS monitor using stats.js.
     */
    setupStats() {
        this.statsFPS = new Stats();
        this.statsFPS.showPanel(0); // FPS panel
        this.statsFPS.dom.style.position = 'fixed';
        this.statsFPS.dom.style.top = '10px';
        this.statsFPS.dom.style.left = '10px';
        this.statsFPS.dom.style.zIndex = '100';
        document.body.appendChild(this.statsFPS.dom);

        this.statsMS = new Stats();
        this.statsMS.showPanel(1); // MS/frame panel
        this.statsMS.dom.style.position = 'fixed';
        this.statsMS.dom.style.top = '50px';   // move below FPS panel
        this.statsMS.dom.style.left = '10px';
        this.statsMS.dom.style.zIndex = '100';
        document.body.appendChild(this.statsMS.dom);

        this.statsMem = new Stats();
        this.statsMem.showPanel(2); // Memory panel
        this.statsMem.dom.style.position = 'fixed';
        this.statsMem.dom.style.top = '90px'; // move below MS panel
        this.statsMem.dom.style.left = '10px';
        this.statsMem.dom.style.zIndex = '100';
        document.body.appendChild(this.statsMem.dom);
    }

    /**
     * Adds optional visual debugging tools: grid and axes helpers.
     */
    visualizeGrid(size = 100, divisions = 50) {
        this.gridHelper = new THREE.GridHelper(size, divisions, 0x888888, 0x444444);
        this.scene.add(this.gridHelper);

        this.axesHelper = new THREE.AxesHelper(10);
        this.scene.add(this.axesHelper);
    }

}
