import { SceneManager } from './SceneManager.js';

const sceneManager = new SceneManager();

/**
 * Called by Kotlin via evaluateJavaScript to send JSON data.
 */
window.receiveFromApp = function(json) {
    try {
        const data = JSON.parse(json);
        handleAppMessage(data);
    } catch (e) {
        sendToApp({ type: 'error', message: 'Invalid JSON from app: ' + json })
    }
};

/**
 * Handles messages from Kotlin.
 */
function handleAppMessage(data) {
    if (data.type === 'updateCubeScale' && data.scale) {
        sceneManager.updateCubeScale(data.scale);
        sendToApp({ type: 'success', message: 'Cube scale updated' })
    } else {
        sendToApp({ type: 'error', message: 'Unknown message type: ' + JSON.stringify(data) });
    }
}

/**
 * Sends data back to the Kotlin app.
 */
function sendToApp(data) {
    const json = JSON.stringify(data);
    if (window.webkit?.messageHandlers?.GameEvent) {
        window.webkit.messageHandlers.GameEvent.postMessage({ params: json });
    } else if (window.GameEvent) {
        window.GameEvent.postMessage({ params: json });
    } else {
        console.warn('No JS bridge available to send message:', json);
    }
}
