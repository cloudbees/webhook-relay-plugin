# Overview

The Webhook Relay system publishes hooks (from an external webhook based system (e.g. Github / Dockerhub)) to subscribers on an internal network. This is primarily used for secure systems which do not expose their Jenkins instance to the outside world.

There are two components in the webhook-relay universe:

* webhook-relay - Docker container running a Python application that listens for hook events and publishes
* webhook-relay-plugin - Jenkins plugin that connects via websockets to the webhook-relay, downloads the events and then sends them to the Jenkins instance.

# webhook-relay

The code for the webhook-relay and information on configuration can be found in <other repo>

## Network Architecture

* **Publisher** - Github / Dockerhub running on the public network
* **Relay** - runs on a publically accessible network (e.g. network DMZ)
* **Subscriber** - Jenkins running the webhook-relay plugin

## Protocols

* **Publisher to Relay** - HTTP or HTTPS
* **Relay** - the relay is currently a single process that share publish / subscribe duties
* **Subscriber to Relay** - Websockets over HTTP (WS) or HTTPS (WSS)

### Configuration

Start Jenkins with an environment variable `WEBHOOK_SUBSCRIPTION` which tells the plugin the endpoint to connect to.

See the documentation for the `webhook-relay` for more information on how this URL is constructed and used.

### Troubleshooting

#### Logging

You can enable FINEST logging to check if payload are received and see the actual payload and request headers by add a log recorder for `org.jenkinsci.plugins.webhookrelay` under *Manage Jenkins > System Logs*. 

#### Reconnect to the Webhook Relay

If the `WebhookRelayManager` loses its connection and seem to not be receiving events anymore, you can reconnect it by executing the following Groovy script under *Manage Jenkins > Script Console*.

```
import org.jenkinsci.plugins.webhookrelay.*
WebhookRelayManager.getInstance().reconnect(WebhookRelayStorage.relayURI);
```