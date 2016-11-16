package org.jenkinsci.plugins.webhookconnector;

import hudson.Extension;
import hudson.model.PeriodicWork;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;
import java.util.concurrent.Future;
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
                while (true) { listen(); }
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
    private static void listen() {
        WebSocketClient client = new WebSocketClient();
        //String destUri = "wss://cloudbees-hooksocket.beescloud.com/ws?tenant=java";
        //String destUri = "ws://172.18.128.252:33048/ws?tenant=java";
        String destUri = "ws://localhost:8888/subscribe/testing";


        //String destUri = "ws://localhost:8888/ws?tenant=java";

        WebhookReceiver socket = new WebhookReceiver();
        try
        {
            client.start();
            URI echoUri = new URI(destUri);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            Future<Session> fs = client.connect(socket,echoUri,request);
            System.out.printf("Attempting to subscribe for webhooks to : %s%n",echoUri);

            try {
                fs.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.println("Failure to connect. Will try again shortly.. " + e.getMessage());
                Thread.sleep(5000);
                return;
            }

            socket.await();
            System.out.println("Peace out...");

        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        finally
        {
            try
            {
                client.stop();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

}
