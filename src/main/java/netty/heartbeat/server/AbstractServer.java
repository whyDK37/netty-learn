package netty.heartbeat.server;

import netty.heartbeat.AbstractEndpoint;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

public class AbstractServer extends AbstractEndpoint {

    ExecutorService executor;
    private InetSocketAddress localAddress;
    private InetSocketAddress bindAddress;
    private int accepts;
    private int idleTimeout;

    public AbstractServer(String bindIp, int bindPort, int timeout, long idleTimeout, long connectTimeout) {
        super(timeout, idleTimeout, connectTimeout);

        bindAddress = new InetSocketAddress(bindIp, bindPort);
    }


    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }
}
