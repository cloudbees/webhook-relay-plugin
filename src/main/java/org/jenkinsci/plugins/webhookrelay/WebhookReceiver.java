package org.jenkinsci.plugins.webhookrelay;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

//MN see: https://www.eclipse.org/jetty/documentation/9.3.x/jetty-websocket-client-api.html


public class WebhookReceiver extends WebSocketClient {
    private static final Logger LOGGER = Logger.getLogger(WebsocketHandler.class.getName());

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
        LOGGER.info("webhook-relay-plugin.onOpen: " + serverUri);
    }

    @Override
    public void onMessage(String message) {
        LOGGER.info("webhook-relay-plugin.onMessage: " + message);

        JSONObject json = JSONObject.fromObject(message);
        JSONObject headers = json.getJSONObject("headers");
        String body = json.getString("body");

        HttpClient client = HttpClientBuilder.create().build();
        String postback = (Boolean.getBoolean("hudson.hpi.run"))? "/jenkins/github-webhook/" : "/github-webhook/";

        HttpPost post = new HttpPost(postback);
        String contentType = "application/json";

        for (Object k : headers.names()) {
            String headerName = (String) k;
            String header = (String) headers.get(headerName);
            if (headerName.equalsIgnoreCase("content-type")) {
                contentType = header;
            }
            if (!headerName.equalsIgnoreCase("content-length")) {
                post.setHeader(headerName, header);
            }
        }

        post.setHeader("User-Agent", "webhook-relay-plugin");
        post.setEntity(new StringEntity(body, ContentType.create(contentType)));


        HttpResponse res;
        try {
            res = client.execute(new HttpHost(InetAddress.getLocalHost(), 8080), post);
        } catch (IOException e) {
            LOGGER.warning(String.format("Error posting back webhook: %s", e.getMessage()));
            throw new RuntimeException(e);
        }
        LOGGER.info(String.format("Result from post back: %s", res.toString()));

    }

    @Override
    public void onClose(int i, String s, boolean b) {
        LOGGER.info(String.format("Websocket Connection closed: %d - %s%n will try reconnect again soon.", i, s));
        this.closeLatch.countDown();

    }

    @Override
    public void onError(Exception e) {
        LOGGER.fine("Client error from websocket");
        this.closeLatch.countDown();
    }

}
