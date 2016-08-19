package io.socket2;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import org.json.JSONObject;

/**
 * @author rjhy
 * @created on 16-8-18
 * @desc desc
 */
public class SocketIO {
    private IOCallback callback;
    private IOConnection connection;
    private String namespace;
    private String url;
    private SSLContext sslContext;

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
        return callback;
    }

    public void setCallback(IOCallback callback) {
        this.callback = callback;
    }

    public SocketIO() {

    }

    public SocketIO(String url) {
        this.url = url;
    }

    public SocketIO(String url, IOCallback callback) {
        this.url = url;
        this.callback = callback;
    }

    public SocketIO(String url, Properties headers) {
        this.url = url;
        this.headers = headers;
    }

    public void setDefaultSSLSocketFactory(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public boolean connect() {
        if (url == null || callback == null) {
            throw new RuntimeException("url and callback may not be null.");
        }
        try {
            URL tempUrl = new URL(url);
            final String origin = tempUrl.getProtocol() + "://"
                    + tempUrl.getAuthority();
            this.namespace = tempUrl.getPath();
            if (this.namespace.equals("/")) {
                this.namespace = "";
            }
            this.connection = IOConnection.register(origin, this);
            if (sslContext != null) {
                this.connection.setSslContext(sslContext);
            }
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
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

    public void send(String message) {
        this.connection.send(this, null, message);
    }

    public void send(IOAcknowledge ack, String message) {
        this.connection.send(this, ack, message);
    }

    public void send(JSONObject json) {
        this.connection.send(this, null, json);
    }

    public void send(IOAcknowledge ack, JSONObject json) {
        this.connection.send(this, ack, json);
    }

    public void emit(String event, Object... args) {
        this.connection.emit(this, event, null, args);
    }

    public void emit(String event, IOAcknowledge ack,
            Object... args) {
        this.connection.emit(this, event, ack, args);
    }
}
