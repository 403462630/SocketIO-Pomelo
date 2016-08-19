package io.socket2;

import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author rjhy
 * @created on 16-8-18
 * @desc desc
 */
public class IOPackageFactory {
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

    public static IOPackage buildConnectPacket(String endPoint) {
        return new IOPackage(TYPE_CONNECT, endPoint, "");
    }

    public static IOPackage buildDisconnectPacket(String endPoint) {
        return new IOPackage(TYPE_DISCONNECT, endPoint, "");
    }

    public static IOPackage buildHeartbeatPacket() {
        return new IOPackage(TYPE_HEARTBEAT, "", "");
    }

    public static IOPackage buildPackagePacket(String endPoint, String text) {
        return new IOPackage(TYPE_MESSAGE, endPoint, text);
    }

    public static IOPackage buildJsonPackagePacket(String endPoint, JSONObject json) {
        return new IOPackage(TYPE_JSON_MESSAGE, endPoint, json.toString());
    }

    public static IOPackage buildEventPacket(String endPoint, String eventName, Object... args) {
        JSONObject json = null;
        try {
            json = new JSONObject().put("name", eventName).put("args", new JSONArray(
                    Arrays.asList(args)));
            return new IOPackage(TYPE_EVENT, endPoint, json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static IOPackage buildAckPacket(String endPoint, String text) {
        return new IOPackage(TYPE_ACK, endPoint, text);
    }
}
