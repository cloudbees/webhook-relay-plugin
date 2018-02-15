/**
 * Created by michaelneale on 26/5/17.
 */
package org.jenkinsci.plugins.webhookrelay

import hudson.Extension

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Create a persistent connection to the webhook forwarding remote service.
 */
class WebsocketHandler {

    private var relayURI: String? = null
    private var receiver: WebhookReceiver? = null
    private val LOGGER = Logger.getLogger(WebsocketHandler::class.java.name)

    fun disconnectFromRelay() {
        this.relayURI = null
        if (receiver != null) {
            try {
                receiver!!.closeBlocking()
            } catch (e: InterruptedException) {
                LOGGER.log(Level.FINE, "Failure disconnecting from relay", e)
            }

        }
    }

    fun connectToRelay(relayURI: String) {
        LOGGER.info("Connecting to " + relayURI)
        this.relayURI = relayURI

        try {
            val sslContext = sslContext()

            Thread(Runnable {
                while (true) {
                    try {
                        listen(sslContext.socketFactory)
                        Thread.sleep(5000)
                    } catch (e: Exception) {
                        LOGGER.log(Level.WARNING, e.message, e)
                        try {
                            Thread.sleep(10000) // In the event of something catastrophic - just backoff a little
                        } catch (ignore: InterruptedException) {
                            LOGGER.fine("Interrupted listening")
                        }

                    }

                }
            }).start()

        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    private fun sslContext(): SSLContext {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, null, null) // Use Java's default key and trust store which is sufficient unless you deal with self-signed certificates
        return sslContext
    }


    /**
     * This will connect to the remove service, and block.
     * Once the connection is over, it returns. You can just establish it again (in fact this is what you should do).
     * The WebhookReceiver handles what happens when an event comes in.
     */
    private fun listen(sslSocketFactory: SSLSocketFactory) {
        receiver = WebhookReceiver(URI(relayURI!!))

        try {
            if (relayURI!!.startsWith("wss://")) {
                receiver!!.setSocket(sslSocketFactory.createSocket())
            }

            receiver!!.connectBlocking() //wait for connection to be established
            if (!receiver!!.connection.isOpen) {
                LOGGER.info("UNABLE TO ESTABLISH WEBSOCKET CONNECTION FOR WEBHOOK. Will back of and try later")
                Thread.sleep(10000)
                return
            }
            receiver!!.await() //block here until it is closed, or errors out
        } finally {
            receiver!!.close()
            receiver = null
        }
    }


}
