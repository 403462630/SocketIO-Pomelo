package io.socket2;

import io.socket.SocketIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.ssl.SSLContext;

/**
 * @author rjhy
 * @created on 16-8-18
 * @desc desc
 */
public class SocketIO implements IOCallback{

    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECT = 3;

    private IOConnection connection;
    private String namespace;
    private String url;
    private SSLContext sslContext;
    private SocketIOStrategy strategy;
    private SocketIOFactory factory;
    private int state = STATE_DISCONNECT;

    private final Collection<ConnectionListener> connectionListeners = new CopyOnWriteArrayList();
    private final Collection<IOMessageListener> messageListeners = new CopyOnWriteArrayList();
    private final Collection<EventListener> eventListeners = new CopyOnWriteArrayList();

    public void addConnectionListener(ConnectionListener listener) {
        if (connectionListeners.contains(listener)) {
            return;
        }
        this.connectionListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener connectionListener) {
        this.connectionListeners.remove(connectionListener);
    }

    public void addIOMessageListener(IOMessageListener listener) {
        if (messageListeners.contains(listener)) {
            return;
        }
        this.messageListeners.add(listener);
    }

    public void removeIOMessageListener(IOMessageListener listener) {
        this.messageListeners.remove(listener);
    }

    public void addEventListener(EventListener listener) {
        if (eventListeners.contains(listener)) {
            return;
        }
        this.eventListeners.add(listener);
    }

    public void removeEventListener(EventListener listener) {
        this.eventListeners.remove(listener);
    }

    /** Used for setting header during handshaking. */
    private Properties headers = new Properties();

    public String getNamespace() {
        return namespace;
    }

    public Properties getHeaders() {
        return headers;
    }

    void setHeaders(Properties headers) {
        this.headers = headers;
    }

    public IOCallback getCallback() {
        return this;
    }

    public void setStrategy(SocketIOStrategy strategy) {
        if (this.connection != null) {
            throw new RuntimeException("You may only set strategy before connecting");
        }
        this.strategy = strategy;
    }

    public void setFactory(SocketIOFactory factory) {
        this.factory = factory;
    }

    public SocketIOFactory getFactory() {
        return factory;
    }

    public SocketIO() {

    }

    public SocketIO(String url) {
        this.url = url;
    }

    public SocketIO(String url, Properties headers) {
        this.url = url;
        this.headers = headers;
    }

    public void setDefaultSSLSocketFactory(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public boolean connect() {
        if (url == null) {
            throw new RuntimeException("url may not be null.");
        }
        try {
            URL tempUrl = new URL(url);
            final String origin = tempUrl.getProtocol() + "://"
                    + tempUrl.getAuthority();
            this.namespace = tempUrl.getPath();
            if (this.namespace.equals("/")) {
                this.namespace = "";
            }
            this.state = STATE_CONNECTING;
            this.connection = IOConnection.register(origin, this);
            if (sslContext != null) {
                this.connection.setSslContext(sslContext);
            }
            if (strategy != null) {
                this.connection.setStrategy(strategy);
            }
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isConnected() {
        if ("".equals(namespace)) {
            return connection.isConnected();
        } else {
            return state == STATE_CONNECTED;
        }
    }

    public boolean isConnecting() {
        return !isConnected() && !isDisconnet();
    }

    public boolean isDisconnet() {
        return state == STATE_DISCONNECT;
    }

    public boolean reconnect() {
        return connect();
    }

    public void disconnect() {
        this.connection.unregister(this);
    }

    public SocketIO addHeader(String key, String value) {
        if (this.connection != null) {
            throw new RuntimeException(
                    "You may only set headers before connecting.\n"
                            + " Try to use new SocketIO().addHeader(key, value).connect(host, callback) "
                            + "instead of SocketIO(host, callback).addHeader(key, value)");
        }
        this.headers.setProperty(key, value);
        return this;
    }

    public String getHeader(String key) {
        if (this.headers.contains(key)) {
            return this.headers.getProperty(key);
        }
        return null;
    }

    public void sendIOMessage(IOMessage message) {
        this.connection.sendIOMessage(this, null, message);
    }

    public void sendIOMessage(IOAcknowledge ack, IOMessage message) {
        this.connection.sendIOMessage(this, ack, message);
    }

    public void emit(String event, Object... args) {
        this.connection.emit(this, event, null, args);
    }

    public void emit(String event, IOAcknowledge ack,
            Object... args) {
        this.connection.emit(this, event, ack, args);
    }

    @Override
    public void onDisconnect() {
        state = STATE_DISCONNECT;
    }

    @Override
    public void onConnect() {
        state = STATE_CONNECTED;
    }

    @Override
    public void onConnectFailed() {
        state = STATE_DISCONNECT;
    }

    @Override
    public void onMessage(IOMessage message, IOAcknowledge ack) {
        for (IOMessageListener listener : messageListeners) {
            listener.onMessage(message, ack);
        }
    }

    @Override
    public void onMessage(List<IOMessage> messages, IOAcknowledge ack) {
        for (IOMessageListener listener : messageListeners) {
            listener.onMessage(messages, ack);
        }
    }

    @Override
    public void onMessageFailed(IOMessage message) {
        for (IOMessageListener listener : messageListeners) {
            listener.onMessageFailed(message);
        }
    }

    @Override
    public void on(String event, IOAcknowledge ack, Object... args) {
        for (EventListener listener : eventListeners) {
            listener.on(event, ack, args);
        }
    }

    @Override
    public void onError(SocketIOException socketIOException) {

    }

    public static interface ConnectionListener{
        void onDisconnect();

        void onConnect();

        void onConnectFailed();
    }

    public static interface IOMessageListener {
        void onMessage(IOMessage message, IOAcknowledge ack);

        void onMessage(List<IOMessage> messages, IOAcknowledge ack);

        void onMessageFailed(IOMessage message);
    }

    public static interface EventListener{
        /**
         * On [Event]. Called when server emits an event.
         *
         * @param event Name of the event
         * @param ack an {@link io.socket.IOAcknowledge} instance, may be <code>null</code> if there's none
         * @param args Arguments of the event
         */
        void on(String event, IOAcknowledge ack, Object[] args);
    }
}
