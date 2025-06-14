import * as THREE from 'three';

export class Settlement {
    constructor(position, playerId, playerColor = 0xffffff) {
        this.position = position;
        this.playerId = playerId;
        this.playerColor = playerColor;
        this.isCity = false; // Can be upgraded later
        this.mesh = this.createMesh();
    }

    createMesh() {
        const group = new THREE.Group();
        group.position.set(this.position.x, 0, this.position.z);

        // Base (cube)
        const baseGeometry = new THREE.BoxGeometry(1.2, 0.8, 1.2);
        const baseMaterial = new THREE.MeshPhongMaterial({
            color: this.playerColor,
            shininess: 30
        });
        const base = new THREE.Mesh(baseGeometry, baseMaterial);
        base.position.y = 0.4;
        group.add(base);

        // Roof (pyramid)
        const roofHeight = 1.2;
        const roofGeometry = new THREE.ConeGeometry(0.9, roofHeight, 4);
        roofGeometry.rotateY(Math.PI / 4); // Square base alignment
        const roofMaterial = new THREE.MeshPhongMaterial({
            color: 0x222222,
            shininess: 10
        });
        const roof = new THREE.Mesh(roofGeometry, roofMaterial);
        roof.position.y = 0.8 + roofHeight/2;
        group.add(roof);

        // Highlight edges
        const edges = new THREE.LineSegments(
            new THREE.EdgesGeometry(baseGeometry),
            new THREE.LineBasicMaterial({ color: 0x000000, linewidth: 2 })
        );
        edges.position.y = 0.4;
        group.add(edges);

        // Add slight variation for visual interest
        group.rotation.y = Math.random() * Math.PI / 8;

        return group;
    }

    upgradeToCity() {
        if (this.isCity) return;

        // Scale up existing base
        this.mesh.children[0].scale.set(1.3, 1.3, 1.3);

        // Add second story
        const storyGeometry = new THREE.BoxGeometry(0.8, 0.8, 0.8);
        const storyMaterial = new THREE.MeshPhongMaterial({
            color: this.playerColor,
            shininess: 30
        });
        const story = new THREE.Mesh(storyGeometry, storyMaterial);
        story.position.set(0, 1.2, 0);
        this.mesh.add(story);

        // Add roof details
        const roofDetailGeometry = new THREE.CylinderGeometry(0.3, 0.5, 0.2, 4);
        roofDetailGeometry.rotateY(Math.PI / 4);
        const roofDetail = new THREE.Mesh(
            roofDetailGeometry,
            new THREE.MeshPhongMaterial({ color: 0xaa0000 })
        );
        roofDetail.position.set(0, 2.1, 0);
        this.mesh.add(roofDetail);

        this.isCity = true;
    }

    serialize() {
        return {
            type: 'SETTLEMENT',
            position: {
                x: this.position.x,
                y: this.position.y,
                z: this.position.z
            },
            playerId: this.playerId,
            isCity: this.isCity
        };
    }
}