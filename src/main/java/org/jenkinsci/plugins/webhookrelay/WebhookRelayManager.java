package org.jenkinsci.plugins.webhookrelay;

import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;


public class WebhookRelayManager {

    private static final Logger LOGGER = Logger.getLogger(WebhookRelayManager.class.getName());

    private static WebhookRelayManager instance = null;
    private WebsocketHandler websocketHandler = null;

    private WebhookRelayManager() {
    }

    public static WebhookRelayManager getInstance() {
        if (instance == null) {
            instance = new WebhookRelayManager();
        }
        return instance;
    }

    public void reconnect(String relayURI) {

        if (websocketHandler != null) {
            websocketHandler.disconnectFromRelay();
        }

        if (StringUtils.isBlank(relayURI)) {
            LOGGER.warning("WebhookRelay not connected as no relay URI configured");
            return;
        }

        websocketHandler = new WebsocketHandler(relayURI);
        websocketHandler.connectToRelay();
    }

}
