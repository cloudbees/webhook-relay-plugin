package org.jenkinsci.plugins.webhookconnector;

import jenkins.model.Jenkins;
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

//MN see: https://www.eclipse.org/jetty/documentation/9.3.x/jetty-websocket-client-api.html


public class WebhookReceiver extends WebSocketClient {

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
        System.out.printf("Got connect: %s", serverUri);
    }

    @Override
    public void onMessage(String message) {

        HttpClient client = HttpClientBuilder.create().build();
        String postback = (Boolean.getBoolean("hudson.hpi.run")) ? "/jenkins/github-webhook/" : "/github-webhook/";
        System.out.println(postback);
        HttpPost post = new HttpPost(postback);

        post.setHeader("User-Agent", "bastion-master");
        post.setHeader("X-Github-Event", "push");
        post.setEntity(new StringEntity(message, ContentType.create("application/x-www-form-urlencoded")));


        HttpResponse res = null;
        try {
            res = client.execute(new HttpHost(InetAddress.getLocalHost(), 8080), post);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(res.toString());
        System.out.println("Got msg:" + message);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        System.out.printf("Connection closed: %d - %s%n", i, s);
        this.closeLatch.countDown();

    }

    @Override
    public void onError(Exception e) {
        System.out.println("Client error");
        e.printStackTrace();
        this.closeLatch.countDown();
    }

}
