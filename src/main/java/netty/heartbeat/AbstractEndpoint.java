package netty.heartbeat;

public class AbstractEndpoint {


    private int timeout;
    private long connectTimeout;
    private long idleTimeout;

    public AbstractEndpoint(int timeout, long idleTimeout, long connectTimeout) {
        this.timeout = timeout;
        this.idleTimeout = idleTimeout;
        this.connectTimeout = connectTimeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }
}
