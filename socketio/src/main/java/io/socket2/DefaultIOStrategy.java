package io.socket2;

/**
 * @author rjhy
 * @created on 16-8-22
 * @desc desc
 */
public class DefaultIOStrategy implements SocketIOStrategy {

    @Override
    public boolean isAutoReconnectHandler() {
        return true;
    }

    @Override
    public Long maxAutoReconnectCount() {
        return Long.valueOf(60);
    }

    @Override
    public long reconnectPeriod() {
        return 5_000;
    }

    @Override
    public boolean isheartBeat() {
        return false;
    }

    @Override
    public boolean isTimeoutHandler() {
        return true;
    }

    @Override
    public boolean isResendHandler() {
        return false;
    }

    @Override
    public int maxResendCount() {
        return 3;
    }

    @Override
    public long timeout() {
        return 60_000;
    }
}
