/*
 * socket.io-java-client IOCallback.java
 *
 * Copyright (c) 2012, ${author}
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
package io.socket2;

import io.socket.SocketIOException;
import java.util.List;
import org.json.JSONObject;

/**
 * The Interface IOCallback. A callback interface to SocketIO
 */
public interface IOCallback {

    /**
     * On disconnect. Called when the socket disconnects and there are no further attempts to
     * reconnect
     */
    void onDisconnect();

    /**
     * On connect. Called when the socket becomes ready so it is now able to receive data
     */
    void onConnect();

    void onConnectFailed();

    void onMessage(IOMessage message, IOAcknowledge ack);

    void onMessage(List<IOMessage> messages, IOAcknowledge ack);

    void onMessageFailed(IOMessage message);

    /**
     * On [Event]. Called when server emits an event.
     *
     * @param event Name of the event
     * @param ack an {@link io.socket.IOAcknowledge} instance, may be <code>null</code> if there's
     * none
     * @param args Arguments of the event
     */
    void on(String event, IOAcknowledge ack, Object... args);

    /**
     * On error. Called when socket is in an undefined state. No reconnect attempts will be made.
     *
     * @param socketIOException the last exception describing the error
     */
    void onError(SocketIOException socketIOException);
}
