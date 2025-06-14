import * as THREE from 'three';

export class HexTile {
    constructor(config) {
        this.type = config.type;
        this.position = config.position;
        this.token = config.token;
        this.mesh = this.createMesh();
    }

    // In your HexTile class
    createMesh() {
        const geometry = new THREE.CircleGeometry(1, 6);
        const material = new THREE.MeshStandardMaterial({
            color: this.getColorByType(),
            roughness: 0.3,
            metalness: 0.1,
            side: THREE.DoubleSide
        });

        const mesh = new THREE.Mesh(geometry, material);

        // CORRECTED axial to world coordinates
        const size = 1.75;
        const hexWidth = Math.sqrt(3) * size;
        const hexHeight = 2 * size;

        // Proper isometric projection
        const x = size * (Math.sqrt(3) * this.position.q + Math.sqrt(3)/2 * this.position.r);
        const z = size * (3/2 * this.position.r);

        mesh.position.set(x, 0.1, z); // Keep slight Y offset to avoid z-fighting
        return mesh;
    }

    getColorByType() {
        const colors = {
            'forest': 0x228B22,
            'hills': 0xCD5C5C,
            'mountains': 0x808080,
            'fields': 0xFFFF00,
            'pasture': 0x98FB98,
            'desert': 0xF4A460
        };
        return colors[this.type] || 0x0000FF;
    }

    serialize() {
        return {
            type: this.type,
            position: this.position,
            token: this.token
        };
    }
}