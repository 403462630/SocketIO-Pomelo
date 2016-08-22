package com.baidao.pomelo;

import android.util.Log;
import io.socket2.IOAcknowledge;
import io.socket2.IOMessage;
import io.socket2.SocketIO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author rjhy
 * @created on 16-8-22
 * @desc desc
 */
public class PomeloClient {
    private final static String TAG = "PomeloClient";
    private final static String URLHEADER = "http://";
    private SocketIO socket;
    private int sequence;

    private Map<String, Callback> callBackMap = new HashMap<>();
    //private Map<String, List<DataListener>> listeners;
    private SocketIO.IOMessageListener messageListener = new SocketIO.IOMessageListener() {
        @Override
        public void onMessage(IOMessage message, IOAcknowledge ack) {
            onMessageSuccess((PomeloMessage) message);
        }

        @Override
        public void onMessage(List<IOMessage> messages, IOAcknowledge ack) {
            for (IOMessage message : messages) {
                onMessageSuccess((PomeloMessage) message);
            }
        }

        @Override
        public void onMessageFailed(IOMessage message) {
            PomeloClient.this.onMessageFailed((PomeloMessage) message);
        }
    };

    private SocketIO.EventListener eventListener = new SocketIO.EventListener() {
        @Override
        public void on(String event, IOAcknowledge ack, Object[] args) {
            Log.i(TAG, "------EVENT: " + event);
        }
    };

    private synchronized int nextSequence() {
        if (sequence == Integer.MAX_VALUE) {
            sequence = 0;
        }
        return ++sequence;
    }

    public PomeloClient(String url, int port) {
        initSocket(url, port);
    }

    private void initSocket(String url, int port) {
        StringBuffer buff = new StringBuffer();
        if (!url.contains(URLHEADER)) {
            buff.append(URLHEADER);
        }
        buff.append(url);
        buff.append(":");
        buff.append(port);
        socket = new SocketIO(buff.toString());
        socket.setFactory(new PomeloFactory());
        socket.setStrategy(new PomeleStrategy());
        addIOMessageListener(messageListener);
        addEventListener(eventListener);
    }

    public void connect() {
        socket.connect();
    }

    public void disconnect() {
        socket.disconnect();
    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    public void notify(String route, JSONObject msg) {
        sendMessage(route, msg, null);
    }

    public void sendMessage(String route, JSONObject msg, Callback callback) {
        int id = nextSequence();
        if (callback != null) {
            callBackMap.put(String.valueOf(id), callback);
        }
        msg = filter(msg);
        socket.sendIOMessage(new PomeloMessage(id, route, msg));
    }

    public void emit(String eventName, JSONObject message) {
        socket.emit(eventName, message);
    }

    private JSONObject filter(JSONObject msg) {
        if (msg == null) {
            msg = new JSONObject();
        }
        long date = System.currentTimeMillis();
        try {
            msg.put("timestamp", date);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return msg;
    }

    private void onMessageSuccess(PomeloMessage message) {
        if (message.getReqId() != null) {
            Callback cb = callBackMap.get(String.valueOf(message.getReqId()));
            cb.onSuccess(message.getText());
            callBackMap.remove(message.getReqId());
        } else {

        }
    }

    private void onMessageFailed(PomeloMessage message) {
        //if (message == null) {
        //    return ;
        //}
        if (message.getReqId() != null) {
            Callback cb = callBackMap.get(String.valueOf(message.getReqId()));
            cb.onError();
            callBackMap.remove(message.getReqId());
        }
    }

    public void addIOMessageListener(SocketIO.IOMessageListener listener) {
        socket.addIOMessageListener(listener);
    }

    public void removeIOMessageListener(SocketIO.IOMessageListener listener) {
        socket.removeIOMessageListener(listener);
    }

    public void addEventListener(SocketIO.EventListener listener) {
        socket.addEventListener(listener);
    }

    public void removeEventListener(SocketIO.EventListener listener) {
        socket.removeEventListener(listener);
    }

    public void addConnectionListener(SocketIO.ConnectionListener listener) {
        socket.addConnectionListener(listener);
    }

    public void removeConnectionListener(SocketIO.ConnectionListener listener) {
        socket.removeConnectionListener(listener);
    }
}
