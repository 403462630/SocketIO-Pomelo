package io.socket2;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author rjhy
 * @created on 16-8-18
 * @desc desc
 */
class TransportManager {
    private IOConnection connection;
    private IOTransport transport;
    private TransportWriter transportWriter;

    private HashMap<String, IOPacketTask> map = new HashMap<>();

    public TransportManager(IOConnection connection) {
        this.connection = connection;
        this.transportWriter = new TransportWriter(this);
    }

    public void setTransport(IOTransport transport) {
        this.transport = transport;
    }

    public void sendIOPackage(IOPackage message) {
        if (message != null) {
            if (connection.getStrategy().isTimeoutHandler() && message.hasTimeoutTask()) {
                stopTimeoutTask(message);
                startTimeoutTask(message, connection.getStrategy().timeout());
            }
            transportWriter.sendIOPackage(message);
        }
    }

    private void startTimeoutTask(IOPackage message, long timeout) {
        IOPacketTask task = new IOPacketTask(this, message, timeout);
        task.execute();
        putTimeoutTask(task);
    }

    private void stopTimeoutTask(IOPackage message) {
        IOPacketTask task = map.get(message.getPrimaryKey());
        if (task != null) {
            removeTimeoutTast(task.getTaskId());
            task.cancel();
        }
    }

    void removeTimeoutTast(String taskId) {
        map.remove(taskId);
    }

    void putTimeoutTask(IOPacketTask task) {
        String key = task.getTaskId();
        if (key != null) {
            map.put(key, task);
        }
    }

    public void start() {
        transportWriter.setTransport(transport);
        transportWriter.start();
    }

    public void stop() {
        transportWriter.stop();
    }

    public void clearup() {
        transportWriter.clearup();
        map.clear();
    }

    public void handleClearup(ArrayList<IOPackage> list) {
        if (list != null && !list.isEmpty()) {
            for (IOPackage message : list) {
                stopTimeoutTask(message);
                connection.handleFailedIOPackage(message);
            }
        }
    }

    public void handlePushFailed(IOPackage message) {
        if (connection.getStrategy().isTimeoutHandler() && message.hasTimeoutTask()) {
            stopTimeoutTask(message);
        }
        if (connection.getStrategy().isResendHandler()) {
            resendIOPackage(message);
        } else {
            connection.handleFailedIOPackage(message);
        }
    }

    private void resendIOPackage(IOPackage message) {
        if (message.getFailedCount() < connection.getStrategy().maxResendCount()
                && message.hasResendHandler()) {
            message.setFailedCount(message.getFailedCount() + 1);
            sendIOPackage(message);
        } else {
            connection.handleFailedIOPackage(message);
        }
    }

    public void handleSendBulkError(ArrayList<IOPackage> list) {
        if (list != null && !list.isEmpty()) {
            for (IOPackage message : list) {
                if (connection.getStrategy().isTimeoutHandler() && message.hasTimeoutTask()) {
                    stopTimeoutTask(message);
                }
                if (connection.getStrategy().isResendHandler()) {
                    resendIOPackage(message);
                } else {
                    connection.handleFailedIOPackage(message);
                }
            }
        }
    }

    public void handleSendError(IOPackage message) {
        if (connection.getStrategy().isTimeoutHandler() && message.hasTimeoutTask()) {
            stopTimeoutTask(message);
        }
        if (connection.getStrategy().isResendHandler()) {
            resendIOPackage(message);
        } else {
            connection.handleFailedIOPackage(message);
        }
    }

    public void handleTimeOuPackage(IOPackage message) {
        removeTimeoutTast(message.getPrimaryKey());
        connection.handleFailedIOPackage(message);
    }

    public void handleReceiverIOPackage(IOPackage message) {
        if (connection.getStrategy().isTimeoutHandler() && message.hasTimeoutTask()) {
            stopTimeoutTask(message);
        }
    }
}
