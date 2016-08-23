package io.socket2;

import io.socket.SocketIOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author rjhy
 * @created on 16-8-18
 * @desc desc
 */
public class IOConnection {
    /** Debug logger */
    static final Logger logger = Logger.getLogger("io.socket-IOConnection");

    /** Socket.io path. */
    public static final String SOCKET_IO_1 = "/socket.io/1/";

    private final static String JSONARRAY_FLAG = "[";
    private static final int STATE_INIT = 0;
    private static final int STATE_HANDSHAKE = 1;
    private static final int STATE_CONNECTING = 2;
    private static final int STATE_CONNECTED = 3;
    private static final int STATE_INTERRUPTED = 4;
    private static final int STATE_INVALID = 6;
    private static final int STATE_HANDSHAKE_ERROR = 7;
    private static final int STATE_ON_ERROR = 8;
    private int state = STATE_INIT;

    private SSLContext sslContext = null;
    private static HashMap<String, List<IOConnection>> connections =
            new HashMap<String, List<IOConnection>>();
    private URL url;
    private IOTransport transport;
    private int nextId = 1;
    HashMap<Integer, IOAcknowledge> acknowledge = new HashMap<Integer, IOAcknowledge>();
    private HashMap<String, SocketIO> sockets = new HashMap<String, SocketIO>();
    private Properties headers;
    private int connectTimeout = 10000;
    private String sessionId;
    private long heartbeatTimeout;
    private long closingTimeout;
    private List<String> protocols;
    private TransportManager transportManager;
    private HearbeatTimeoutTask heartbeatTimeoutTask;
    private Timer backgroundTimer;
    private ReconnectTask reconnectTask = null;
    private SocketIOStrategy strategy = new DefaultIOStrategy();
    private long reconnectCount;

    public void setStrategy(SocketIOStrategy strategy) {
        this.strategy = strategy;
    }

    SocketIOStrategy getStrategy() {
        return strategy;
    }

    private class ReconnectTask extends TimerTask {

        @Override
        public void run() {
            logger.info("reconnecting....... ");
            boolean handshakeFlag = true;
            if (getState() == STATE_INIT || getState() == STATE_INVALID
                    || getState() == STATE_HANDSHAKE_ERROR || getState() == STATE_ON_ERROR) {
                handshakeFlag = handshake();
            }
            if (handshakeFlag) {
                connectTransport();
            }
        }
    }

    private class HearbeatTimeoutTask extends TimerTask {
        @Override
        public void run() {
            logger.info("heartbeat Timeout ");
            heartbeatTimeout();
        }
    }

    private synchronized void resetTimeout() {
        if (heartbeatTimeoutTask != null) {
            heartbeatTimeoutTask.cancel();
        }
        if (getState() != STATE_INVALID) {
            heartbeatTimeoutTask = new HearbeatTimeoutTask();
            backgroundTimer.schedule(heartbeatTimeoutTask, closingTimeout + heartbeatTimeout);
        }
    }

    /**
     * Returns the session id. This should be called from a {@link io.socket.IOTransport}
     *
     * @return the session id to connect to the right Session.
     */
    public String getSessionId() {
        return sessionId;
    }

    synchronized int getState() {
        return state;
    }

    private synchronized void setState(int state) {
        this.state = state;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public void transportError(Exception ex) {
        //do not thing
    }

    public void transportConnected() {
        setState(STATE_CONNECTED);
        logger.info("transport connected");
        transportManager.setTransport(transport);
        if (reconnectTask != null) {
            reconnectTask.cancel();
            reconnectTask = null;
        }
        transportManager.start();
        socketsConnect();
        resetTimeout();
    }

    public void transportMessage(String text) {
        IOPackage ioPackage;
        try {
            ioPackage = new IOPackage(text);
        } catch (Exception e) {
            logger.info("Garbage from server: " + text + ", " + e.getMessage());
            //error(new SocketIOException("Garbage from server: " + text, e));
            return;
        }
        resetTimeout();
        switch (ioPackage.getType()) {
            case IOPackage.TYPE_DISCONNECT:
                if (findCallback(ioPackage) != null) {
                    findCallback(ioPackage).onDisconnect();
                }
                break;
            case IOPackage.TYPE_CONNECT:
                transportManager.handleReceiverIOPackage(ioPackage);
                findCallback(ioPackage).onConnect();
                break;
            case IOPackage.TYPE_HEARTBEAT:
                sendPlain(IOPackage.buildHeartbeatPacket());
                break;
            case IOPackage.TYPE_MESSAGE:
                if (ioPackage.getData().indexOf(JSONARRAY_FLAG) == 0) {
                    try {
                        JSONArray jsonArray = new JSONArray(ioPackage.getData());
                        List<IOMessage> messages = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            IOMessage ioMessage = findSocketIO(ioPackage).getFactory()
                                    .buildIOMessage(jsonArray.getJSONObject(i).toString(), false);
                            ioPackage.setPrimaryKey(ioMessage.getPrimaryKey());
                            transportManager.handleReceiverIOPackage(ioPackage);
                            messages.add(ioMessage);
                        }
                        findCallback(ioPackage).onMessage(messages, remoteAcknowledge(ioPackage));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    IOMessage ioMessage = findSocketIO(ioPackage).getFactory()
                            .buildIOMessage(ioPackage.getData(), false);
                    ioPackage.setPrimaryKey(ioMessage.getPrimaryKey());
                    transportManager.handleReceiverIOPackage(ioPackage);
                    findCallback(ioPackage).onMessage(ioMessage, remoteAcknowledge(ioPackage));
                }
                break;
            case IOPackage.TYPE_JSON_MESSAGE:
                IOMessage ioMessage = findSocketIO(ioPackage).getFactory()
                        .buildIOMessage(ioPackage.getData(), true);
                ioPackage.setPrimaryKey(ioMessage.getPrimaryKey());
                transportManager.handleReceiverIOPackage(ioPackage);
                findCallback(ioPackage).onMessage(ioMessage, remoteAcknowledge(ioPackage));
                break;
            case IOPackage.TYPE_EVENT:
                try {
                    JSONObject event = new JSONObject(ioPackage.getData());
                    Object[] argsArray;
                    if (event.has("args")) {
                        JSONArray args = event.getJSONArray("args");
                        argsArray = new Object[args.length()];
                        for (int i = 0; i < args.length(); i++) {
                            if (args.isNull(i) == false) {
                                argsArray[i] = args.get(i);
                            }
                        }
                    } else {
                        argsArray = new Object[0];
                    }
                    String eventName = event.getString("name");
                    findCallback(ioPackage).on(eventName, remoteAcknowledge(ioPackage), argsArray);
                } catch (JSONException e) {
                    logger.warning("Malformated JSON received: " + e.getMessage());
                }
                break;
            case IOPackage.TYPE_ACK:
                String[] data = ioPackage.getData().split("\\+", 2);
                if (data.length == 2) {
                    try {
                        int id = Integer.parseInt(data[0]);
                        IOAcknowledge ack = acknowledge.get(id);
                        if (ack == null) {
                            logger.warning("Received unknown ack packet");
                        } else {
                            JSONArray array = new JSONArray(data[1]);
                            Object[] args = new Object[array.length()];
                            for (int i = 0; i < args.length; i++) {
                                args[i] = array.get(i);
                            }
                            ack.ack(args);
                        }
                    } catch (NumberFormatException e) {
                        logger.warning(
                                "Received malformated Acknowledge! This is potentially filling up the acknowledges!");
                    } catch (JSONException e) {
                        logger.warning("Received malformated Acknowledge data!");
                    }
                } else if (data.length == 1) {
                    sendPlain(IOPackage.buildAckPacket(ioPackage.getEndPoint(), data[0]));
                }
                break;
            case IOPackage.TYPE_ERROR:
                findCallback(ioPackage).onError(new SocketIOException(ioPackage.getData()));

                if (ioPackage.getData().endsWith("+0")) {
                    // We are advised to disconnect
                    setState(STATE_ON_ERROR);
                    //connect();
                    //cleanup();
                }
                break;
            case IOPackage.TYPE_NOOP:
                break;
            default:
                logger.warning("Unkown type received" + ioPackage.getType());
                break;
        }
    }

    public void transportDisconnected() {
        transportManager.stop();
        if (getState() != STATE_INVALID) {
            if (getState() != STATE_ON_ERROR) {
                setState(STATE_INTERRUPTED);
            }
            reconnect();
        }
    }

    public void transportDisconnectError(Exception e) {
        //do not thing
    }

    private class ConnectThread extends Thread {
        public ConnectThread() {
            super("ConnectThread");
        }

        @Override
        public void run() {
            logger.info("connecting....... ");
            if (handshake()) {
                connectTransport();
            }
        }
    }

    private void heartbeatTimeout() {
        cleanup();
    }

    private void invalidateTransport() {
        if (transport != null) {
            transport.invalidate();
        }
        transport = null;
    }

    private synchronized void connect() {
        if (getState() == STATE_INIT || getState() == STATE_INVALID) {
            if (backgroundTimer != null) {
                backgroundTimer.cancel();
            }
            backgroundTimer = new Timer("backgroundTimer");
            new ConnectThread().start();
        } else if (getState() == STATE_INTERRUPTED || getState() == STATE_HANDSHAKE_ERROR
                || getState() == STATE_ON_ERROR) {
            invalidateTransport();
            transportManager.stop();
            if (reconnectTask != null) {
                reconnectTask.cancel();
            }
            reconnectTask = new ReconnectTask();
            backgroundTimer.schedule(reconnectTask, strategy.reconnectPeriod());
        }
    }

    private synchronized void reconnect() {
        if (strategy.isAutoReconnectHandler() && (strategy.maxAutoReconnectCount() == null
                || strategy.maxAutoReconnectCount() >= reconnectCount)) {
            reconnectCount++;
            connect();
        } else {
            cleanup();
        }
    }

    private void socketsConnect() {
        for (SocketIO socket : sockets.values()) {
            if (!"".equals(socket.getNamespace())) {
                sendPlain(IOPackage.buildConnectPacket(socket.getNamespace()));
            }
        }
    }

    private synchronized void connectTransport() {
        if (getState() == STATE_INVALID) {
            return;
        }
        setState(STATE_CONNECTING);
        if (protocols.contains(WebsocketTransport.TRANSPORT_NAME)) {
            transport = WebsocketTransport.create(url, this);
        } else if (protocols.contains(XhrTransport.TRANSPORT_NAME)) {
            transport = XhrTransport.create(url, this);
        } else {
            logger.info(
                    "connectTransport error: Server supports no available transports. You should reconfigure the server to support a available transport");
            cleanup();
            return;
        }
        transport.connect();
    }

    private boolean handshake() {
        URL url;
        String response;
        URLConnection connection;
        try {
            setState(STATE_HANDSHAKE);
            url = new URL(IOConnection.this.url.toString() + SOCKET_IO_1);
            connection = url.openConnection();
            if (connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(
                        sslContext.getSocketFactory());
            }
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(connectTimeout);

			/* Setting the request headers */
            for (Map.Entry<Object, Object> entry : headers.entrySet()) {
                connection.setRequestProperty((String) entry.getKey(), (String) entry.getValue());
            }

            InputStream stream = connection.getInputStream();
            Scanner in = new Scanner(stream);
            response = in.nextLine();
            String[] data = response.split(":");
            logger.info("handshake success--response: " + response);
            sessionId = data[0];
            heartbeatTimeout = Long.parseLong(data[1]) * 1000;
            closingTimeout = Long.parseLong(data[2]) * 1000;
            protocols = Arrays.asList(data[3].split(","));
            return true;
        } catch (Exception e) {
            logger.info("handshake Exception: " + e.getMessage());
            handshakeError();
            return false;
        }
    }

    private void handshakeError() {
        setState(STATE_HANDSHAKE_ERROR);
        reconnect();
    }

    private synchronized void sendPlain(IOPackage message) {
        if (getState() != STATE_INVALID && getState() != STATE_INIT) {
            transportManager.sendIOPackage(message);
        } else {
            if (message.getType() == IOPackage.TYPE_CONNECT) {
                connect();
                transportManager.sendIOPackage(message);
            } else {
                handleFailedIOPackage(message);
            }
        }
    }

    public void sendIOMessage(SocketIO socket, IOAcknowledge ack, IOMessage message) {
        IOPackage ioPackage = IOPackage.buildIOMessagePacket(socket.getNamespace(), message);
        synthesizeAck(ioPackage, ack);
        sendPlain(ioPackage);
    }

    public void emit(SocketIO socket, String event, IOAcknowledge ack, Object... args) {
        try {
            IOPackage message = IOPackage.buildEventPacket(socket.getNamespace(), event, args);
            synthesizeAck(message, ack);
            sendPlain(message);
        } catch (JSONException e) {
            //error(new SocketIOException(
            //        "Error while emitting an event. Make sure you only try to send arguments, which can be serialized into JSON."));
            logger.warning("emit error: " + e.getMessage());
        }
    }

    private void synthesizeAck(IOPackage message, IOAcknowledge ack) {
        if (ack != null) {
            int id = nextId++;
            acknowledge.put(id, ack);
            message.setId(id + "+");
        }
    }

    private IOAcknowledge remoteAcknowledge(IOPackage message) {
        String _id = message.getId();
        if (_id.equals("")) {
            return null;
        } else if (_id.endsWith("+") == false) {
            _id = _id + "+";
        }
        final String id = _id;
        final String endPoint = message.getEndPoint();
        return new IOAcknowledge() {
            @Override
            public void ack(Object... args) {
                JSONArray array = new JSONArray();
                for (Object o : args) {
                    try {
                        array.put(o == null ? JSONObject.NULL : o);
                        IOPackage ackMsg =
                                IOPackage.buildAckPacket(endPoint, id + array.toString());
                        sendPlain(ackMsg);
                    } catch (Exception e) {
                        logger.info(
                                "You can only put values in IOAcknowledge.ack() which can be handled by JSONArray.put()");
                        //error(new SocketIOException(
                        //        "You can only put values in IOAcknowledge.ack() which can be handled by JSONArray.put()",
                        //        e));
                    }
                }
            }
        };
    }

    private IOConnection(String url, SocketIO socket) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        transportManager = new TransportManager(this);
        headers = socket.getHeaders();
        sockets.put(socket.getNamespace(), socket);
        connect();
    }

    /**
     * Creates a new connection or returns the corresponding one.
     *
     * @param origin the origin
     * @param socket the socket
     * @return a IOConnection object
     */
    static public IOConnection register(String origin, SocketIO socket) {
        List<IOConnection> list = connections.get(origin);
        if (list == null) {
            list = new LinkedList<>();
            connections.put(origin, list);
        } else {
            synchronized (list) {
                for (IOConnection connection : list) {
                    if (connection.register(socket)) {
                        return connection;
                    }
                }
            }
        }

        IOConnection connection = new IOConnection(origin, socket);
        list.add(connection);
        return connection;
    }

    /**
     * Connects a socket to the IOConnection.
     *
     * @param socket the socket to be connected
     * @return true, if successfully registered on this transport, otherwise
     * false.
     */
    public synchronized boolean register(SocketIO socket) {
        String namespace = socket.getNamespace();
        if (sockets.containsKey(namespace)) {
            if (sockets.get(namespace) != socket) {
                return false;
            }
        }
        sockets.put(namespace, socket);
        socket.setHeaders(headers);
        IOPackage message = IOPackage.buildConnectPacket(socket.getNamespace());
        sendPlain(message);
        return true;
    }

    /**
     * Disconnect a socket from the IOConnection. Shuts down this IOConnection
     * if no further connections are available for this IOConnection.
     *
     * @param socket the socket to be shut down
     */
    public synchronized void unregister(SocketIO socket) {
        IOPackage message = IOPackage.buildDisconnectPacket(socket.getNamespace());
        sendPlain(message);
        sockets.remove(socket.getNamespace());
        socket.getCallback().onDisconnect();

        if (sockets.size() == 0) {
            cleanup();
        }
    }

    public boolean isConnected() {
        return getState() == STATE_CONNECTED;
    }

    private void cleanup() {
        setState(STATE_INVALID);
        if (transportManager != null) {
            transportManager.clearup();
        }
        if (transport != null) {
            transport.disconnect();
        }
        for (SocketIO socket : sockets.values()) {
            socket.getCallback().onDisconnect();
        }
        synchronized (connections) {
            List<IOConnection> con = connections.get(url.toString());
            if (con != null && con.size() > 1) {
                con.remove(this);
            } else {
                connections.remove(url.toString());
            }
        }
        logger.info("Cleanup");
        sockets.clear();
        backgroundTimer.cancel();
        backgroundTimer = null;
    }

    private IOCallback findCallback(IOPackage message) {
        SocketIO socket = findSocketIO(message);
        if (socket == null) {
            logger.info("Cannot find socket for '" + message.getEndPoint() + "'");
            return null;
        }
        return socket.getCallback();
    }

    SocketIO findSocketIO(IOPackage message) {
        return sockets.get(message.getEndPoint());
    }

    SocketIO findSocketIO(String endPoint) {
        return sockets.get(endPoint);
    }

    void handleFailedIOPackage(IOPackage message) {
        SocketIO socketIO = findSocketIO(message);
        if (socketIO != null) {
            if (message.getType() == IOPackage.TYPE_CONNECT) {
                socketIO.getCallback().onConnectFailed();
            } else if (message.getType() == IOPackage.TYPE_MESSAGE) {
                socketIO.getCallback().onMessageFailed(message.getTempMessage());
            } else if (message.getType() == IOPackage.TYPE_JSON_MESSAGE) {
                socketIO.getCallback().onMessageFailed(message.getTempMessage());
            }
        }
    }
}
