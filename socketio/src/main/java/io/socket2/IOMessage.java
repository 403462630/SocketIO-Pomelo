package io.socket2;

import org.json.JSONObject;

/**
 * @author rjhy
 * @created on 16-8-22
 * @desc desc
 */
public interface IOMessage {

    public String toText();

    public String getPrimaryKey();

    public boolean isJsonText();
}
