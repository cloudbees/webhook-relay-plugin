package org.jenkinsci.plugins.webhookrelay;

import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;


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

    public void reconnect(String relayURI) {

        if (StringUtils.isBlank(relayURI)) {
            //this.bugsnag = null;
            LOGGER.warning("WebhookRelay not connected as no relay URI configured");
            websocketHandler.disconnectFromRelay();
            return;
        }

        websocketHandler.connectToRelay(relayURI);

        LOGGER.info("WebhookRelay exception handler has registered.");
    }

}
