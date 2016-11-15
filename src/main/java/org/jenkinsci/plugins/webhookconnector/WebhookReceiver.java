package org.jenkinsci.plugins.webhookconnector;

import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class WebhookReceiver {

    @OnWebSocketMessage
    public void onMessage(String message) {
        System.out.printf("Got msg: %s%n", message);
    }
}
