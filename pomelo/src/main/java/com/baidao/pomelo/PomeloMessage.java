package com.baidao.pomelo;

import io.socket2.IOMessage;
import org.json.JSONObject;

/**
 * @author rjhy
 * @created on 16-8-22
 * @desc desc
 */
public class PomeloMessage implements IOMessage {
    private Integer reqId;
    private JSONObject text;
    private String route;

    public PomeloMessage(Integer reqId, String route, JSONObject text) {
        this.reqId = reqId;
        this.route = route;
        this.text = text;
    }

    public Integer getReqId() {
        return reqId;
    }

    public JSONObject getText() {
        return text;
    }

    public String getRoute() {
        return route;
    }

    @Override
    public String toText() {
        return Protocol.encode(reqId, route, text.toString());
    }

    @Override
    public String getPrimaryKey() {
        return String.valueOf(reqId);
    }

    @Override
    public boolean isJsonText() {
        return false;
    }
}
