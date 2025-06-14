export class GameState {
    constructor() {
        this.tiles = [];
        this.roads = [];
        this.settlements = [];
        this.players = {};
    }

    addTile(tile) {
        this.tiles.push(tile);
    }

    addSettlement(settlement) {
        this.settlements.push(settlement);
    }

    addRoad(road) {
        this.roads.push(road);
    }

    getStateSnapshot() {
        return {
            tiles: this.tiles.map(t => t.serialize()),
            settlements: this.settlements.map(s => s.serialize()),
            roads: this.roads.map(r => r.serialize())
        };
    }
}