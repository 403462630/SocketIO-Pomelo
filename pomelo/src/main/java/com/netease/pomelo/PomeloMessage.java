package com.netease.pomelo;

import io.socket2.IOMessage;

/**
 * @author rjhy
 * @created on 16-8-22
 * @desc desc
 */
public class PomeloMessage implements IOMessage {

    private Integer reqId;
    private String text;
    private String route;

    public PomeloMessage(Integer reqId, String route, String text) {
        this.reqId = reqId;
        this.route = route;
        this.text = text;
    }

    public Integer getReqId() {
        return reqId;
    }

    public String getText() {
        return text;
    }

    public String getRoute() {
        return route;
    }

    @Override
    public String toText() {
        return Protocol.encode(reqId, route, text);
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
