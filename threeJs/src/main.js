import { SceneManager } from './core/SceneManager.js';
import { MessageHandler } from './communication/MessageHandler.js';

// Initialize core systems
const sceneManager = new SceneManager();

// Set up communication handler
const messageHandler = new MessageHandler(sceneManager);

// Expose to Kotlin
window.receiveFromApp = (json) => messageHandler.handleIncoming(json);