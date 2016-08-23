package io.socket2;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * @author rjhy
 * @created on 16-8-18
 * @desc desc
 */
public class IOPacketTask {
    static final Logger logger = Logger.getLogger("io.socket-IOPacketTask");
    static final long DEFAULT_TIME_OUT = 60 * 1000;
    private IOPackage message;
    private Timer timer = new Timer();
    private TransportManager transportManager;
    private long timeout;

    public IOPacketTask(TransportManager transportManager, IOPackage message) {
        this(transportManager, message, DEFAULT_TIME_OUT);
    }

    public IOPacketTask(TransportManager transportManager, IOPackage message, long timeout) {
        this.timeout = timeout;
        this.transportManager = transportManager;
        this.message = message;
    }

    public void execute() {
        if (message == null) {
            return;
        }
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                logger.info(
                        "send text time out, message = " + message.getType() + ":" + message.getId()
                                + ":" + message.getEndPoint());
                message.setExpread(true);
                transportManager.handleTimeOuPackage(message);
            }
        };
        timer.schedule(task, timeout);
    }

    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public String getTaskId() {
        return message == null ? null : message.getPrimaryKey();
    }
}
