package org.jenkinsci.plugins.webhookrelay;

import hudson.Extension;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Create a persistent connection to the webhook forwarding remote service.
 */
public class WebsocketHandler {
    private static final Logger LOGGER = Logger.getLogger(WebsocketHandler.class.getName());
    private String relayURI;
    private static WebsocketHandler instance;


    private WebhookReceiver receiver;

    public void disconnectFromRelay() {
        this.relayURI = null;
        if (receiver != null) {
            try {
                receiver.closeBlocking();
            } catch (InterruptedException e) {
                // I doubt we care
                LOGGER.log(Level.FINE, "Failure disconnecting from relay", e);
            }
        }
    }

    public void connectToRelay(String relayURI) {
        LOGGER.info("Connecting to " + relayURI);
        this.relayURI = relayURI;

        try {
            //XXX I think Jenkins has a globally configured SSL factory thingmajig
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null); // Use Java's default key and trust store which is sufficient unless you deal with self-signed certificates

            final SSLSocketFactory socketFactor = sslContext.getSocketFactory();

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            listen(socketFactor);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, e.getMessage(), e);
                            try {
                                Thread.sleep(10000); // In the event of something catastrophic - just backoff a little
                            } catch (InterruptedException ignore) {
                            }
                        }
                    }
                }
            });
            t.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * This will connect to the remove service, and block.
     * Once the connection is over, it returns. You can just establish it again (in fact this is what you should do).
     * The WebhookReceiver handles what happens when an event comes in.
     */
    private void listen(SSLSocketFactory sslSocketFactory) throws URISyntaxException, InterruptedException, NoSuchAlgorithmException, KeyManagementException, IOException {

        receiver = new WebhookReceiver(new URI(relayURI));

        try {
            if (relayURI.startsWith("wss://")) {
                receiver.setSocket(sslSocketFactory.createSocket());
            }

            receiver.connectBlocking(); //wait for connection to be established
            if (!receiver.getConnection().isOpen()) {
                LOGGER.info("UNABLE TO ESTABLISH WEBSOCKET CONNECTION FOR WEBHOOK. Will back of and try later");
                Thread.sleep(10000);
                return;
            }
            receiver.await(); //block here until it is closed, or errors out
        } finally {
            receiver = null;
        }
    }

}
