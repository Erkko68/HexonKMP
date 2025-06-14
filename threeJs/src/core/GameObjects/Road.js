import * as THREE from 'three';

export class Road {
    constructor(startPoint, endPoint, playerId, playerColor = 0xffffff) {
        this.start = startPoint;
        this.end = endPoint;
        this.playerId = playerId;
        this.playerColor = playerColor;
        this.mesh = this.createMesh();
    }

    createMesh() {
        const group = new THREE.Group();

        // Calculate road direction vector
        const direction = new THREE.Vector3().subVectors(this.end, this.start);
        const length = direction.length();
        const center = new THREE.Vector3().addVectors(this.start, this.end).multiplyScalar(0.5);

        // Create road segment
        const geometry = new THREE.BoxGeometry(0.5, 0.2, length);
        const material = new THREE.MeshPhongMaterial({
            color: this.playerColor,
            shininess: 20
        });
        const road = new THREE.Mesh(geometry, material);

        // Position at center point
        road.position.copy(center);
        road.position.y = 0.1;

        // Rotate to face direction
        road.rotation.y = Math.atan2(direction.x, direction.z);
        group.add(road);

        // Add wooden texture details
        const plankCount = Math.floor(length / 0.8);
        for (let i = 0; i < plankCount; i++) {
            const plank = new THREE.Mesh(
                new THREE.BoxGeometry(0.4, 0.05, 0.1),
                new THREE.MeshPhongMaterial({ color: 0x8b4513 })
            );
            plank.position.set(
                0,
                0.15,
                (i / (plankCount - 1) - 0.5) * length * 0.9
            );
            plank.rotation.x = Math.PI / 8 * (Math.random() - 0.5);
            road.add(plank);
        }

        return group;
    }

    serialize() {
        return {
            type: 'ROAD',
            start: {
                x: this.start.x,
                y: this.start.y,
                z: this.start.z
            },
            end: {
                x: this.end.x,
                y: this.end.y,
                z: this.end.z
            },
            playerId: this.playerId
        };
    }
}