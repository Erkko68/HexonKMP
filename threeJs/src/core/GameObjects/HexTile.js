import * as THREE from 'three';

export class HexTile {
    constructor(config) {
        this.type = config.type;
        this.position = config.position; // Should be {q, r} axial coordinates
        this.token = config.token;
        this.mesh = this.createMesh();
    }

    createMesh() {
        const geometry = new THREE.CircleGeometry(1, 6); // Radius 1, 6 segments for hexagon
        const material = new THREE.MeshStandardMaterial({
            color: this.getColorByType(),
            roughness: 0.3,
            metalness: 0.1,
            side: THREE.DoubleSide
        });

        const mesh = new THREE.Mesh(geometry, material);

        // Convert axial coordinates (q, r) to world coordinates (x, z)
        const size = 1.0; // Radius of the hexagon
        const spacing = 1.0; // Spacing between hexagons

        mesh.receiveShadow = true;

        // Standard axial to world coordinate conversion (flat-top hexagons)
        const x = size * (3/2) * this.position.q * spacing;
        const z = size * (Math.sqrt(3)/2 * this.position.q + Math.sqrt(3) * this.position.r) * spacing;

        mesh.position.set(x, 0, z); // Y position is 0 for flat layout
        mesh.rotation.x = -Math.PI / 2; // Rotate to lie flat on XZ plane

        // Slightly elevate to avoid z-fighting with grid
        mesh.position.y = 0.01;

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