package com.netease.pomelo;

import io.socket2.IOMessage;
import io.socket2.SocketIOFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author rjhy
 * @created on 16-8-22
 * @desc desc
 */
public class PomeloFactory implements SocketIOFactory {

    @Override
    public IOMessage createHeartbeat() {
        return null;
    }

    @Override
    public IOMessage buildIOMessage(String text, boolean isJsonText) {
        try {
            JSONObject jsonObject = new JSONObject(text);
            Integer id = null;
            String body = null;
            String route = null;
            if (jsonObject.has("id")) {
                id = jsonObject.getInt("id");
            }
            if (jsonObject.has("body")) {
                body = jsonObject.getJSONObject("body").toString();
            }
            if (jsonObject.has("route")) {
                route = jsonObject.getString("route");
            }
            return new PomeloMessage(id, route, body);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
