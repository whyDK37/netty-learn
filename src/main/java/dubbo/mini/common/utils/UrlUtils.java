package dubbo.mini.common.utils;

import dubbo.mini.common.Constants;
import dubbo.mini.common.URL;

public class UrlUtils {

    public static int getHeartbeat(URL url) {
        return url.getParameter(Constants.HEARTBEAT_KEY, Constants.DEFAULT_HEARTBEAT);
    }

    public static int getIdleTimeout(URL url) {
        int heartBeat = getHeartbeat(url);
        int idleTimeout = url.getParameter(Constants.HEARTBEAT_TIMEOUT_KEY, heartBeat * 3);
        if (idleTimeout < heartBeat * 2) {
            throw new IllegalStateException("idleTimeout < heartbeatInterval * 2");
        }
        return idleTimeout;
    }
}
