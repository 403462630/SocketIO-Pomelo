package io.socket2;

/**
 * @author rjhy
 * @created on 16-8-17
 * @desc desc
 */
public class IOPackage {

    /** 发送失败次数  */
    private int failedCount;
    /** 是否已过期 */
    private boolean isExpread;

    final boolean isExpread() {
        return isExpread;
    }

    final void setExpread(boolean expread) {
        isExpread = expread;
    }

    final void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    final int getFailedCount() {
        return failedCount;
    }

    final boolean hasTimeoutTask() {
        return  (type == IOPackageFactory.TYPE_MESSAGE
                || type == IOPackageFactory.TYPE_JSON_MESSAGE
                || type == IOPackageFactory.TYPE_CONNECT)
                && getPrimaryKey() != null;
    }

    final boolean hasResendHandler() {
        return type == IOPackageFactory.TYPE_MESSAGE
                || type == IOPackageFactory.TYPE_JSON_MESSAGE
                || type == IOPackageFactory.TYPE_EVENT;
    }

    private int type;

    private String id;

    private String endPoint;

    private String data;

    public IOPackage(int type, String id, String endPoint, String data) {
        this.type = type;
        this.id = id;
        this.endPoint = endPoint;
        this.data = data;
    }

    public IOPackage(int type, String endPoint, String data) {
        this(type, null, endPoint, data);
    }

    public IOPackage(String message) {
        String[] fields = message.split(":", 4);
        for (int i = 0; i < fields.length; i++) {
            if (i == 0) {
                this.type = Integer.parseInt(fields[i]);
            } else if (i == 1) {
                this.id = fields[i];
            } else if (i == 2) {
                this.endPoint = fields[i];
            } else if (i == 3) {
                this.data = fields[i];
            }
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public String getData() {
        return data;
    }

    public String getPrimaryKey() {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(':').append(type);

        builder.append(':');
        if (id != null) {
            builder.append(id);
        }

        builder.append(':');
        if (endPoint != null) {
            builder.append(endPoint);
        }

        if (!(type == IOPackageFactory.TYPE_HEARTBEAT || type == IOPackageFactory.TYPE_DISCONNECT || type == IOPackageFactory.TYPE_CONNECT)) {
            builder.append(':');
            if (data != null) {
                builder.append(data);
            }
        }

        return builder.substring(1);
    }
}
