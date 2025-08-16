import { SceneManager } from './core/SceneManager.js';
import { MessageHandler } from './communication/MessageHandler.js';
import { BridgeService } from "./communication/BridgeService.js";

// Initiate Communication Bridge with Kotlin
const bridgeService = new BridgeService();

const sceneManager = new SceneManager(document.body,bridgeService);
const messageHandler = new MessageHandler(sceneManager,bridgeService);

// Expose to Kotlin
window.receiveFromApp = (json) => messageHandler.handleIncoming(json);