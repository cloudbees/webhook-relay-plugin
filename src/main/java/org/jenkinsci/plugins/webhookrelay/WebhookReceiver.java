package org.jenkinsci.plugins.webhookrelay;

import jenkins.model.Jenkins;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

//MN see: https://www.eclipse.org/jetty/documentation/9.3.x/jetty-websocket-client-api.html


public class WebhookReceiver extends WebSocketClient {
    private static final Logger LOGGER = Logger.getLogger(WebhookReceiver.class.getName());

    private static String rootUrl = System.getProperty(WebhookReceiver.class.getName() + ".rootUrl");

    private final CountDownLatch closeLatch;
    private final URI serverUri;
    public WebhookReceiver(URI serverUri) {
        super(serverUri);
        this.serverUri = serverUri;
        closeLatch = new CountDownLatch(1);
    }

    public void await() {
        try {
            closeLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        LOGGER.fine("webhook-relay-plugin.onOpen: " + serverUri);
    }

    @Override
    public void onMessage(String message) {
        LOGGER.finest("webhook-relay-plugin.onMessage: " + message);

        JSONObject json = JSONObject.fromObject(message);
        JSONObject headers = json.getJSONObject("headers");
        String body = json.getString("body");
        String postBack = json.getString("requestPath");
        
        HttpClient client = HttpClientBuilder.create().build();

        String baseUrl = rootUrl != null ? rootUrl : Jenkins.getInstance().getRootUrl();
        HttpPost post = new HttpPost(baseUrl + postBack);
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

        // for debug purpose, try to extract some useful infos from received event :
        if (body != null && postBack != null && postBack.startsWith("/github-webhook")) {
            try {
                JSONObject bodyJson = JSONObject.fromObject(body);
                JSONObject repo = bodyJson.getJSONObject("repository");
                if (repo.getString("full_name") != null) {
                    LOGGER.fine("Event received on project : " + repo.getString("full_name"));
                }
            } catch (JSONException e) {
                LOGGER.log(Level.SEVERE, "Unable to extract info from JSON received. Is it a valid JSON ?", e);
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
        LOGGER.finest(String.format("Result from post back: %s", res.toString()));

    }

    @Override
    public void onClose(int i, String s, boolean b) {
        LOGGER.fine(String.format("Websocket Connection closed: %d - %s%n will try reconnect again soon.", i, s));
        this.closeLatch.countDown();

    }

    @Override
    public void onError(Exception e) {
        LOGGER.log(Level.SEVERE, "Client error from websocket", e);
        this.closeLatch.countDown();
    }

    /**
     * Exclude "Content-Length" and "Host".
     */
    public boolean shouldBeIncluded(String header) {
        return !header.equalsIgnoreCase("content-length") && !header.equalsIgnoreCase("Host");

    }

}
