export class BridgeService {
    sendToApp(data) {
        const json = JSON.stringify(data);
        if (window.kmpJsBridge && window.kmpJsBridge.callNative) {
            window.kmpJsBridge.callNative("GameEvent", json, null);
        } else {
            console.warn("Native bridge not available", data);
        }
    }

    sendAck(message) {
        this.sendToApp({ status: 'SUCCESS', message });
    }

    sendError(context, details) {
        this.sendToApp({
            status: 'ERROR',
            error: context,
            details
        });
    }
}