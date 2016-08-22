package io.socket2;

import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author rjhy
 * @created on 16-8-17
 * @desc desc
 */
public class IOPackage {
    /** Message type disconnect */
    public static final int TYPE_DISCONNECT = 0;

    /** Message type connect */
    public static final int TYPE_CONNECT = 1;

    /** Message type heartbeat */
    public static final int TYPE_HEARTBEAT = 2;

    /** Message type message */
    public static final int TYPE_MESSAGE = 3;

    /** Message type JSON message */
    public static final int TYPE_JSON_MESSAGE = 4;

    /** Message type event */
    public static final int TYPE_EVENT = 5;

    /** Message type acknowledge */
    public static final int TYPE_ACK = 6;

    /** Message type error */
    public static final int TYPE_ERROR = 7;

    /** Message type noop */
    public static final int TYPE_NOOP = 8;

    private final static String JSONARRAY_FLAG = "[";
    /** 发送失败次数  */
    private int failedCount;
    /** 是否已过期 */
    private boolean isExpread;

    final boolean isExpread() {
        return isExpread;
    }

    final void setExpread(boolean expread) {
        isExpread = expread;
    }

    final void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    final int getFailedCount() {
        return failedCount;
    }

    final boolean hasTimeoutTask() {
        return  (type == TYPE_MESSAGE
                || type == TYPE_JSON_MESSAGE
                || type == TYPE_CONNECT)
                && getPrimaryKey() != null;
    }

    final boolean hasResendHandler() {
        return type == TYPE_MESSAGE
                || type == TYPE_JSON_MESSAGE
                || type == TYPE_EVENT;
    }

    private int type;

    private String id;

    private String endPoint;

    private IOMessage tempMessage;
    private String data;

    private String primaryKey;

    public IOPackage(int type, String id, String endPoint, IOMessage message) {
        this(type, id, endPoint, message.toText(), message.getPrimaryKey());
        this.tempMessage = message;
    }

    public IOPackage(int type, String id, String endPoint, String data, String primaryKey) {
        this.type = type;
        this.id = id;
        this.endPoint = endPoint;
        this.data = data;
        this.primaryKey = primaryKey;
    }

    public IOPackage(int type, String endPoint, IOMessage message) {
        this(type, null, endPoint, message);
    }

    public IOPackage(String message) {
        String[] fields = message.split(":", 4);
        for (int i = 0; i < fields.length; i++) {
            if (i == 0) {
                this.type = Integer.parseInt(fields[i]);
            } else if (i == 1) {
                this.id = fields[i];
            } else if (i == 2) {
                this.endPoint = fields[i];
            } else if (i == 3) {
                this.data = fields[i];
            }
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public String getData() {
        return data;
    }

    public IOMessage getTempMessage() {
        return tempMessage;
    }

    void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    String getPrimaryKey() {
        if (type == TYPE_CONNECT) {
            return endPoint + "_" + "TYPE_CONNECT";
        } else {
            return primaryKey;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(':').append(type);

        builder.append(':');
        if (id != null) {
            builder.append(id);
        }

        builder.append(':');
        if (endPoint != null) {
            builder.append(endPoint);
        }

        if (!(type == TYPE_HEARTBEAT || type == TYPE_DISCONNECT || type == TYPE_CONNECT)) {
            builder.append(':');
            if (data != null) {
                builder.append(data);
            }
        }

        return builder.substring(1);
    }

    public static IOPackage buildConnectPacket(String endPoint) {
        return new IOPackage(TYPE_CONNECT, endPoint, null);
    }

    public static IOPackage buildDisconnectPacket(String endPoint) {
        return new IOPackage(TYPE_DISCONNECT, endPoint, null);
    }

    public static IOPackage buildHeartbeatPacket() {
        return new IOPackage(TYPE_HEARTBEAT, "", null);
    }

    public static IOPackage buildIOMessagePacket(String endPoint, IOMessage message) {
        if (message.isJsonText()) {
            return new IOPackage(TYPE_JSON_MESSAGE, null, endPoint, message);
        } else {
            return new IOPackage(TYPE_MESSAGE, null, endPoint, message);
        }
    }

    public static IOPackage buildEventPacket(String endPoint, String eventName, Object... args)
            throws JSONException {
        final JSONObject json = new JSONObject().put("name", eventName).put("args", new JSONArray(
                    Arrays.asList(args)));
        return new IOPackage(TYPE_EVENT, endPoint, new IOMessageAdapter() {
            @Override
            public String toText() {
                return json.toString();
            }
        });
    }

    public static IOPackage buildAckPacket(String endPoint, final String text) {
        return new IOPackage(TYPE_ACK, endPoint, new IOMessageAdapter() {
            @Override
            public String toText() {
                return text;
            }
        });
    }

}
