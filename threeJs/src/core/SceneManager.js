import * as THREE from 'three';
import { BoardManager } from './BoardManager.js';

export class SceneManager {
    constructor(container = document.body) {
        this.container = container;
        this.scene = new THREE.Scene();

        this.setupCamera();
        this.setupRenderer();
        this.setupEventListeners();
        this.boardManager = new BoardManager(this.scene);

        this.initScene();
        this.animate();
    }

    setupCamera() {
        const aspect = window.innerWidth / window.innerHeight;
        const viewSize = 15;

        this.camera = new THREE.OrthographicCamera(
            -viewSize * aspect,
            viewSize * aspect,
            viewSize,
            -viewSize,
            0.1,
            1000
        );

        // Isometric position looking at center (0,0,0)
        this.camera.position.set(20, 20, 20);
        this.camera.lookAt(0, 0, 0);
        this.camera.zoom = 1.5;
        this.camera.updateProjectionMatrix();
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

    initScene() {
        // Isometric grid
        const gridSize = 100;
        const gridGeometry = new THREE.PlaneGeometry(gridSize, gridSize, 20, 20);
        const gridMaterial = new THREE.MeshBasicMaterial({
            wireframe: true,
            color: 0x555555,
            opacity: 0.3,
            transparent: true
        });
        this.grid = new THREE.Mesh(gridGeometry, gridMaterial);
        this.grid.rotation.order = 'YXZ';
        this.grid.rotation.y = -Math.PI / 2;
        this.grid.rotation.x = -Math.PI / 2;
        this.scene.add(this.grid);

        // Lighting
        this.scene.add(new THREE.AmbientLight(0xffffff, 0.6));

        const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
        directionalLight.position.set(1, 1, 1).normalize();
        this.scene.add(directionalLight);
    }

    onWindowResize() {
        const aspect = window.innerWidth / window.innerHeight;
        const viewSize = 15;

        this.camera.left = -viewSize * aspect;
        this.camera.right = viewSize * aspect;
        this.camera.top = viewSize;
        this.camera.bottom = -viewSize;
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