package io.socket2;

/**
 * @author rjhy
 * @created on 16-8-22
 * @desc desc
 */
public interface SocketIOStrategy {
    /**
     * 是否有自动重连机制
     *
     * @return boolean
     */
    public boolean isAutoReconnectHandler();

    /**
     * 最大自动重连次数 NULL 表示没有次数限制
     *
     * @return Long
     */
    public Long maxAutoReconnectCount();

    /**
     * 自动重连时间间隔
     *
     * @return long
     */
    public long reconnectPeriod();

    /**
     * 是否有自定义心跳机制， 注意socketio自带心跳（心跳间隔25s），且无法修改
     *
     * @return boolean
     */
    public boolean isheartBeat();

    /**
     * 是否有超时处理机制
     *
     * @return boolean
     */
    public boolean isTimeoutHandler();

    /**
     * 是否有重发机制
     *
     * @return boolean
     */
    public boolean isResendHandler();

    /**
     * 最大重发次数
     *
     * @return int
     */
    public int maxResendCount();

    /**
     * 超时时长
     *
     * @return long
     */
    public long timeout();
}
