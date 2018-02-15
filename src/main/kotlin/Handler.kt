/**
 * Created by michaelneale on 26/5/17.
 */
package org.jenkinsci.plugins.webhookrelay

import kotlinx.coroutines.experimental.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Create a persistent connection to the webhook forwarding remote service.
 */
class WebsocketHandler (val relayURI: String) {

    private val LOGGER = Logger.getLogger(WebsocketHandler::class.java.name)

    private var listener: Job? = null
    private var receiver: WebhookReceiver? = null

    fun disconnectFromRelay() {

        if (listener?.isActive == true) {
            try {
                receiver!!.closeBlocking()

            } catch (e: InterruptedException) {
                LOGGER.log(Level.FINE, "Failure disconnecting from relay", e)

            } finally {
                listener!!.cancel()
            }
        }
        else {
            LOGGER.log(Level.FINE, "No active listener, skipping disconnection.")
        }

    }

    fun connectToRelay() {

        val sslContext = sslContext()

        listener = launch {
            while (true) {
                listen(sslContext.socketFactory)
                delay(5000)
                LOGGER.warning("Unexpected end of listening, retrying ...")
            }
        }

        LOGGER.info("webhook-relay connected to " + relayURI);

    }

    private fun sslContext(): SSLContext {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, null, null) // Use Java's default key and trust store which is sufficient unless you deal with self-signed certificates
        return sslContext
    }


    /**
     * This will connect to the remove service, and block.
     * Once the connection is over, it returns and the parent coroutine restart it after few seconds, indefinitly
     * The WebhookReceiver handles what happens when an event comes in.
     */
    private fun listen(sslSocketFactory: SSLSocketFactory) {
        receiver = WebhookReceiver(URI(relayURI))

        try {
            if (relayURI.startsWith("wss://")) {
                receiver!!.setSocket(sslSocketFactory.createSocket())
            }

            receiver!!.connectBlocking() //wait for connection to be established
            if (!receiver!!.connection.isOpen) {
                return
            }
            receiver!!.await() //block here until it is closed, or errors out
        } finally {
            receiver!!.close()
            receiver = null
        }
    }

}
