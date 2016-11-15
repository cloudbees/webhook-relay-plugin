package org.jenkinsci.plugins.webhookconnector;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;

@WebSocket
public class WebhookReceiver {

    private final CountDownLatch closeLatch;

    public WebhookReceiver() {
        closeLatch = new CountDownLatch(1);
    }

    public void await() {
        try {
            closeLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @OnWebSocketMessage
    public void onMessage(String message) throws IOException {

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("/jenkins/github-webhook/");

        post.setHeader("User-Agent", "bastion-master");
        post.setHeader("X-Github-Event", "push");
        post.setEntity(new StringEntity(message, ContentType.create("application/x-www-form-urlencoded")));


        HttpResponse res = client.execute(new HttpHost(InetAddress.getLocalHost(), 8080), post);
        System.out.println(res.toString());
        System.out.println("Got msg:" + message);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason)
    {
        System.out.printf("Connection closed: %d - %s%n",statusCode,reason);
        this.closeLatch.countDown();
    }

    @OnWebSocketConnect
    public void onConnect(Session session)
    {
        System.out.printf("Got connect: %s%n",session);
    }}
