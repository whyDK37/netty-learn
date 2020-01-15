package dubbo.mini.common.utils;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;

public class UrlUtils {

    public static int getHeartbeat(NetURL url) {
        return url.getParameter(Constants.HEARTBEAT_KEY, Constants.DEFAULT_HEARTBEAT);
    }

    public static int getIdleTimeout(NetURL url) {
        int heartBeat = getHeartbeat(url);
        int idleTimeout = url.getParameter(Constants.HEARTBEAT_TIMEOUT_KEY, heartBeat * 3);
        if (idleTimeout < heartBeat * 2) {
            throw new IllegalStateException("idleTimeout < heartbeatInterval * 2");
        }
        return idleTimeout;
    }
}
