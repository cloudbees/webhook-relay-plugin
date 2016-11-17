package org.jenkinsci.plugins.webhookconnector;

import hudson.Extension;
import hudson.model.PeriodicWork;
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * Create a persistent connection to the webhook forwarding remote service.
 *
 */
@Extension
@SuppressWarnings(value = "unused")
public class WebsocketHandler extends PeriodicWork {

    public WebsocketHandler() {
        super();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        listen();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
    }

    @Override
    protected void doRun() throws Exception {}

    @Override
    public long getRecurrencePeriod() {
        return Long.MAX_VALUE;
    }


    /**
     * This will connect to the remove service, and block.
     * Once the connection is over, it returns. You can just establish it again (in fact this is what you should do).
     * The WebhookReceiver handles what happens when an event comes in.
     */
    private static void listen() throws URISyntaxException, InterruptedException, NoSuchAlgorithmException, KeyManagementException {


        //String destUri = "wss://cloudbees-hooksocket.beescloud.com/subscribe/testing";
        //String destUri = "ws://172.18.128.252:33048/ws?tenant=testing";
        String destUri = System.getenv("WEBHOOK_SUBSCRIPTION");
        if (destUri == null) {
            destUri = "ws://localhost:8888/subscribe/testing";
        }

        SSLContext sslContext = SSLContext.getInstance( "TLS" );
        sslContext.init( null, null, null ); // will use java's default key and trust store which is sufficient unless you deal with self-signed certificates
        WebhookReceiver receiver = new WebhookReceiver(new URI(destUri));
        if (destUri.startsWith("wss://")) {
            receiver.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));
        }

        receiver.connectBlocking();
        receiver.await();


    }

}
