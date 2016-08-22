package io.socket2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * @author rjhy
 * @created on 16-8-18
 * @desc desc
 */
class TransportWriter {
    static final Logger logger = Logger.getLogger("io.socket-PacketWriter");
    private BlockingQueue<IOPackage> queue = new ArrayBlockingQueue<IOPackage>(500, true);
    private static AtomicInteger integer = new AtomicInteger();
    private TransportManager transportManager;
    private IOTransport transport;
    private Thread writerThread;
    private boolean isStop = false;
    private boolean flag = true;

    public TransportWriter(TransportManager transportManager) {
        this.transportManager = transportManager;
    }

    public void setTransport(IOTransport transport) {
        this.transport = transport;
    }

    public void stop() {
        isStop = true;
        flag = true;
        synchronized (queue) {
            queue.notifyAll();
        }
    }

    public void start() {
        isStop = false;
        if (transport == null) {
            throw new RuntimeException("transport is null");
        }
        if (writerThread != null && writerThread.isAlive()) {
            logger.info("WriterThread: " + writerThread.getName() + " is alive");
            synchronized (queue) {
                queue.notifyAll();
            }
            return ;
        }

        writerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                write();
            }
        });
        writerThread.setName("Transport write thread_" + integer.incrementAndGet());
        writerThread.setDaemon(true);
        writerThread.start();
        logger.info("start writerThread: " + writerThread.getName());
    }

    public void clearup() {
        ArrayList<IOPackage> list = nextIOPackageList();
        transportManager.handleClearup(list);
        synchronized (queue) {
            queue.notifyAll();
        }
    }

    public void sendIOPackage(IOPackage message) {
        try {
            if (message != null) {
                queue.put(message);
            }
        } catch (InterruptedException e) {
            logger.info("push message to queue Exception: " + e.getMessage());
            transportManager.handlePushFailed(message);
        }
        if (!isStop) {
            synchronized (queue) {
                queue.notifyAll();
            }
        }
    }

    private IOPackage nextIOPackage() {
        IOPackage ioPackage = null;
        while (!isStop && ((ioPackage = queue.poll()) == null)) {
            try {
                synchronized (queue) {
                    queue.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return ioPackage;
    }

    private ArrayList<IOPackage> nextIOPackageList() {
        ArrayList<IOPackage> list = new ArrayList<>();
        queue.drainTo(list);
        return list;
    }

    private void write() {
        while(!isStop) {
            if (flag && transport.canSendBulk()) {
                flag = false;
                ArrayList<IOPackage> list = nextIOPackageList();
                if (list != null && list.size() > 0) {
                    try {
                        sendBulk(list);
                    } catch (IOException e) {
                        logger.info("writeBulk IOException: " + e.getMessage());
                        transportManager.handleSendBulkError(list);
                    }
                }
            } else {
                IOPackage message = nextIOPackage();
                if (message != null && !message.isExpread()) {
                    String text = message.toString();
                    logger.info("> " + text);
                    try {
                        transport.send(text);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.info("send Exception: " + e.getMessage());
                        transportManager.handleSendError(message);
                    }
                }
            }

            if (isStop) {
                try {
                    logger.info("WriterThread:" + writerThread.getName() + " stop");
                    synchronized (queue) {
                        queue.wait(60_000 * 2);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendBulk(ArrayList<IOPackage> list) throws IOException {
        String[] texts = new String[list.size()];
        logger.info("Bulk start:");
        for (int i = 0; i < list.size(); i++) {
            IOPackage message = list.get(i);
            if (message != null && !message.isExpread()) {
                texts[i] = message.toString();
                logger.info("> " + texts[i]);
            }
        }

        if (texts.length > 0) {
            logger.info("Bulk end");
            transport.sendBulk(texts);
        } else {
            logger.info("Bulk end empty");
        }
    }
}
