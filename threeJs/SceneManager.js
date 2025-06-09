import * as THREE from 'three';

export class SceneManager {
    constructor(container = document.body) {
        this.container = container;
        this.scene = new THREE.Scene();
        this.camera = new THREE.PerspectiveCamera(
            75,
            window.innerWidth / window.innerHeight,
            0.1,
            1000
        );
        this.renderer = new THREE.WebGLRenderer({ antialias: true });
        this.cube = null;

        window.addEventListener('resize', () =>
            {
                this.camera.aspect = window.innerWidth / window.innerHeight;
                this.camera.updateProjectionMatrix();
                this.renderer.setSize(window.innerWidth, window.innerHeight);
                this.renderer.render(this.scene, this.camera);
            }
        )

        this.setupRenderer();
        this.setupScene();
        this.animate();
    }

    setupRenderer() {
        this.renderer.setSize(window.innerWidth, window.innerHeight);
        this.container.appendChild(this.renderer.domElement);
    }

    setupScene() {
        const geometry = new THREE.BoxGeometry();
        const material = new THREE.MeshNormalMaterial();
        this.cube = new THREE.Mesh(geometry, material);
        this.scene.add(this.cube);
        this.camera.position.z = 5;
    }

    animate = () => {
        requestAnimationFrame(this.animate);
        if (this.cube) {
            this.cube.rotation.x += 0.01;
            this.cube.rotation.y += 0.01;
        }
        this.renderer.render(this.scene, this.camera);
    };

    updateCubeScale(scale) {
        if (this.cube && scale) {
            this.cube.scale.set(scale.x, scale.y, scale.z);
        }
    }
}
