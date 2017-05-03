package org.jenkinsci.plugins.webhookrelay;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketTextListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Create a persistent connection to the webhook forwarding remote service.
 */
public class WebhookReceiver {
    private static final Logger LOGGER = Logger.getLogger(WebhookReceiver.class.getName());
    private static final String rootUrl = System.getProperty(WebhookReceiver.class.getName() + ".rootUrl");
    private static final AsyncHttpClientConfig CLIENT_CONFIG = new DefaultAsyncHttpClientConfig.Builder().setWebSocketMaxFrameSize(Integer.MAX_VALUE).build();
    private static final AsyncHttpClient CLIENT = new DefaultAsyncHttpClient(CLIENT_CONFIG);

    private final CountDownLatch closeLatch;
    private final WebSocketUpgradeHandler handler;

    public WebhookReceiver() {
        closeLatch = new CountDownLatch(1);
        handler = newHandler();
    }

    public void connectToRelay(String relayURI) {
        LOGGER.info("Connecting to " + relayURI);

        try {
            Thread t = new Thread(() -> {
                while (true) {
                    try {
                        listen(relayURI);
                        LOGGER.info("RECONNECTING...");
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, e.getMessage(), e);
                        try {
                            Thread.sleep(10000); // In the event of something catastrophic - just backoff a little
                        } catch (InterruptedException ignore) {
                            LOGGER.fine("Interrupted listening");
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
    private void listen(String relayURI) throws URISyntaxException, ExecutionException, InterruptedException, IOException {
        WebSocket ws = CLIENT.prepareGet(relayURI).execute(handler).get();
        try {
            //block here until it is closed, or errors out
            closeLatch.await();
        } finally {
            ws.close();
        }
    }

    /**
     * Make a new websocket callback handler.
     * This should only need to be done once and reused over and over.
     * A latch is used - should a close or an error happen, it unlatches it so that the connection can be renewed.
     */
    private WebSocketUpgradeHandler newHandler() {
        return new WebSocketUpgradeHandler.Builder().addWebSocketListener(
                new WebSocketTextListener() {

                    @Override
                    public void onMessage(String message) {
                        LOGGER.fine("webhook-relay-plugin.onMessage: " + message);
                        applyNotification(message);
                    }

                    @Override
                    public void onOpen(WebSocket websocket) {
                        LOGGER.info("webhook-relay-plugin.onOpen: connection opened");

                    }

                    @Override
                    public void onClose(WebSocket websocket) {
                        LOGGER.fine("Websocket connection closed");
                        closeLatch.countDown();
                    }

                    @Override
                    public void onError(Throwable t) {
                        LOGGER.log(Level.INFO, "webhook-relay-plugin.onError", t);
                        closeLatch.countDown();
                    }
                }).build();
    }


    /**
     * Apply the forwarded payload to Jenkins itself - ie post back to /github-webhook/
     */
    private void applyNotification(String message) {

        JSONObject json = JSONObject.fromObject(message);
        JSONObject headers = json.getJSONObject("headers");
        String body = json.getString("body");

        HttpClient client = HttpClientBuilder.create().build();
        String postback = "github-webhook/";

        String baseUrl = rootUrl != null ? rootUrl : Jenkins.getInstance().getRootUrl();
        HttpPost post = new HttpPost(baseUrl + postback);
        String contentType = "application/json";

        for (Object k : headers.names()) {
            String headerName = (String) k;
            String header = (String) headers.get(headerName);
            if (headerName.equalsIgnoreCase("content-type")) {
                contentType = header;
            }
            if (shouldBeIncluded(headerName)) {
                post.setHeader(headerName, header);
            }
        }

        post.setHeader("User-Agent", "webhook-relay-plugin");
        post.setEntity(new StringEntity(body, ContentType.create(contentType)));


        HttpResponse res;
        try {
            res = client.execute(post);
        } catch (IOException e) {
            LOGGER.warning(String.format("Error posting back webhook: %s", e.getMessage()));
            throw new RuntimeException(e);
        }
        LOGGER.fine(String.format("Result from post back: %s", res.toString()));

    }

    /**
     * Exclude "Content-Length" and "Host".
     */
    private boolean shouldBeIncluded(String header) {
        return !header.equalsIgnoreCase("content-length") && !header.equalsIgnoreCase("Host");

    }

}
