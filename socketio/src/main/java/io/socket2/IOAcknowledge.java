package io.socket2;

import org.json.JSONArray;

/**
 * @author rjhy
 * @created on 16-8-17
 * @desc desc
 */
public interface IOAcknowledge {
    /**
     * Acknowledges a socket.io message.
     *
     * @param args may be all types which can be serialized by {@link JSONArray#put(Object)}
     */
    void ack(Object... args);
}
