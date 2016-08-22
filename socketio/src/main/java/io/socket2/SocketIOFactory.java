package io.socket2;

import org.json.JSONObject;

/**
 * @author rjhy
 * @created on 16-8-22
 * @desc desc
 */
public interface SocketIOFactory {
    public IOMessage createHeartbeat();
    public IOMessage buildIOMessage(String text, boolean isJsonText);
}
