import * as THREE from 'three';
import { BoardManager } from './BoardManager.js';

export class SceneManager {
    constructor(container = document.body) {
        this.container = container;
        this.scene = new THREE.Scene();

        this.setupLights();
        this.setupCamera();
        this.setupRenderer();
        this.setupEventListeners();
        this.boardManager = new BoardManager(this.scene);

        this.visualizeGrid();
        this.animate();
    }

    setupCamera() {
        const aspect = window.innerWidth / window.innerHeight;
        const d = 20;  // Camera frustum size

        // Orthographic camera for isometric view
        this.camera = new THREE.OrthographicCamera(
            -d * aspect,  // left
            d * aspect,   // right
            d,           // top
            -d,          // bottom
            1,          // near
            1000        // far
        );

        // Set camera position for isometric view
        this.camera.position.set(20, 20, 20);
        this.camera.lookAt(0, 0, 0);
    }

    setupLights() {
        // Ambient light for general illumination
        const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
        this.scene.add(ambientLight);

        // Directional light for shadows and highlights
        const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
        directionalLight.position.set(1, 1, 0.5).normalize();
        this.scene.add(directionalLight);
    }

    setupRenderer() {
        this.renderer = new THREE.WebGLRenderer({
            antialias: true,
            alpha: true
        });
        this.renderer.setSize(window.innerWidth, window.innerHeight);
        this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        this.container.appendChild(this.renderer.domElement);
    }

    setupEventListeners() {
        window.addEventListener('resize', this.onWindowResize.bind(this));
    }

    visualizeGrid(size = 100, divisions = 50) {
        this.gridHelper = new THREE.GridHelper(
            size,
            divisions,
            0x888888,
            0x444444
        );
        this.scene.add(this.gridHelper);
        this.axesHelper = new THREE.AxesHelper(10);
        this.scene.add(this.axesHelper);
    }

    onWindowResize() {
        // Update orthographic camera frustum
        const aspect = window.innerWidth / window.innerHeight;
        const d = 20;

        this.camera.left = -d * aspect;
        this.camera.right = d * aspect;
        this.camera.top = d;
        this.camera.bottom = -d;

        this.camera.updateProjectionMatrix();
        this.renderer.setSize(window.innerWidth, window.innerHeight);
    }

    animate() {
        requestAnimationFrame(() => this.animate());
        this.render();
    }

    render() {
        this.renderer.render(this.scene, this.camera);
    }

    generateBoard(boardConfig) {
        this.boardManager.generateBoard(boardConfig);
    }
}