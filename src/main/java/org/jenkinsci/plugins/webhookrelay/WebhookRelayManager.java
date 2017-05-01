package org.jenkinsci.plugins.webhookrelay;

import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketTextListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;


public class WebhookRelayManager {
    private static final Logger LOGGER = Logger.getLogger(WebhookRelayManager.class
            .getName());

    private static WebhookRelayManager instance = null;
    private WebsocketHandler websocketHandler = new WebsocketHandler();

    private WebhookRelayManager() {
    }

    public static WebhookRelayManager getInstance() {
        if (instance == null) {
            instance = new WebhookRelayManager();
        }
        return instance;
    }

    public void reconnect(String relayURI)  {

        AsyncHttpClient c = new DefaultAsyncHttpClient();

        try {
            WebSocket websocket = c.prepareGet(relayURI)
                    .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
                            new WebSocketTextListener() {

                                @Override
                                public void onMessage(String message) {
                                    System.err.println("Message yeah " + message);
                                }

                                @Override
                                public void onOpen(WebSocket websocket) {
                                    System.err.println("open yeah ");
                                }

                                @Override
                                public void onClose(WebSocket websocket) {
                                    System.err.println("closed yeah");
                                }

                                @Override
                                public void onError(Throwable t) {
                                    t.printStackTrace();
                                }
                            }).build()).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }



        /*

        if (StringUtils.isBlank(relayURI)) {
            //this.bugsnag = null;
            LOGGER.warning("WebhookRelay not connected as no relay URI configured");
            websocketHandler.disconnectFromRelay();
            return;
        }

        websocketHandler.connectToRelay(relayURI);

        LOGGER.info("WebhookRelay exception handler has registered.");
        */
    }

}
