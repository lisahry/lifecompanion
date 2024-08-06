package org.lifecompanion.plugin.aac4all.wp2.model.logs;

import java.util.Date;

public class WP2Logs {
    private Date timestamp;
    private LogType type;
    private Object data;

    public WP2Logs(Date timestamp, LogType type, Object data) {
        this.timestamp = timestamp;
        this.type = type;
        this.data = data;
    }
}
