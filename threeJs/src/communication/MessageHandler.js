export class MessageHandler {
    constructor(sceneManager, bridgeService) {
        this.sceneManager = sceneManager;
        this.bridge = bridgeService;
    }

    handleIncoming(json) {
        try {
            const data = JSON.parse(json);
            this.routeMessage(data);
        } catch (e) {
            this.bridge.sendError('Invalid JSON format', json);
        }
    }

    routeMessage(data) {
        try {
            switch (data.type) {
                case 'INIT_BOARD':
                    this.sceneManager.generateBoard(data.config);
                    this.bridge.sendAck('BOARD_CREATED');
                    break;

                //case 'PLACE_SETTLEMENT':
                //    this.sceneManager.placeSettlement(data.position, data.playerId);
                //    this.bridge.sendAck('SETTLEMENT_PLACED');
                //    break;

                //case 'PLACE_ROAD':
                //    this.sceneManager.placeRoad(data.position, data.playerId);
                //    this.bridge.sendAck('ROAD_PLACED');
                //    break;

                //case 'UPDATE_STATE':
                //   // Handle game state updates
                //   break;

                default:
                    this.bridge.sendError('Unknown message type', data.type);
            }
        } catch (e) {
            this.bridge.sendError('Processing error', e.message);
        }
    }
}