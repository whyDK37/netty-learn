package project.fep.server;

import project.fep.AbstractEndpoint;

import java.net.InetSocketAddress;

public class AbstractServer extends AbstractEndpoint {

    private InetSocketAddress bindAddress;

    public AbstractServer(String bindIp, int bindPort, int timeout, long idleTimeout, long connectTimeout) {
        super(timeout, idleTimeout, connectTimeout);

        bindAddress = new InetSocketAddress(bindIp, bindPort);
    }


    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }
}
