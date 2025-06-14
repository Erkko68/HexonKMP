import { HexTile } from './GameObjects/HexTile.js';
import { Settlement } from './GameObjects/Settlement.js';
import { Road } from './GameObjects/Road.js';
import { GameState } from './GameState.js';

export class BoardManager {
    constructor(scene) {
        this.scene = scene;
        this.gameState = new GameState();
    }

    generateBoard(config) {
        // Clear existing board
        this.gameState.tiles.forEach(tile => this.scene.remove(tile.mesh));
        this.gameState = new GameState();

        // Generate new board
        config.tiles.forEach(tileConfig => {
            const tile = new HexTile(tileConfig);
            this.scene.add(tile.mesh);
            this.gameState.addTile(tile);
        });
    }

    placeSettlement(position, playerId) {
        const settlement = new Settlement(position, playerId);
        this.scene.add(settlement.mesh);
        this.gameState.addSettlement(settlement);
    }

    placeRoad(position, playerId) {
        const road = new Road(position, playerId);
        this.scene.add(road.mesh);
        this.gameState.addRoad(road);
    }
}