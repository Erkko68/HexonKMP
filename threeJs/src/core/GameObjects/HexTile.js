import * as THREE from 'three';

export class HexTile {
    constructor(config) {
        this.type = config.type;
        this.position = config.position;
        this.token = config.token;
        this.mesh = this.createMesh();
    }

    createMesh() {
        const geometry = new THREE.CylinderGeometry(1, 1, 0.2, 6);
        const material = new THREE.MeshStandardMaterial({
            color: this.getColorByType(),
            roughness: 0.3,
            metalness: 0.1
        });

        const mesh = new THREE.Mesh(geometry, material);

        // Convert axial (q,r) to isometric world coordinates
        const size = 1.75; // Tile size adjustment
        const x = (this.position.q - this.position.r) * (size * 0.866); // 0.866 = sin(60°)
        const z = (this.position.q + this.position.r) * (size * 0.5);   // 0.5 = cos(60°)

        mesh.position.set(x, 0.1, z);
        mesh.rotation.x = Math.PI / 2;
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