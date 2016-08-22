package com.baidao.pomelo;

import io.socket2.DefaultIOStrategy;

/**
 * @author rjhy
 * @created on 16-8-22
 * @desc desc
 */
public class PomeleStrategy extends DefaultIOStrategy {
    @Override
    public boolean isAutoReconnectHandler() {
        return super.isAutoReconnectHandler();
    }

    @Override
    public Long maxAutoReconnectCount() {
        return super.maxAutoReconnectCount();
    }

    @Override
    public long reconnectPeriod() {
        return super.reconnectPeriod();
    }

    @Override
    public boolean isheartBeat() {
        return super.isheartBeat();
    }

    @Override
    public boolean isTimeoutHandler() {
        return super.isTimeoutHandler();
    }

    @Override
    public boolean isResendHandler() {
        return super.isResendHandler();
    }

    @Override
    public int maxResendCount() {
        return super.maxResendCount();
    }

    @Override
    public long timeout() {
        return 10_000;//super.timeout();
    }
}
