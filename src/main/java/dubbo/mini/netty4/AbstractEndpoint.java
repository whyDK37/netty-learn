package dubbo.mini.netty4;

import dubbo.mini.common.Constants;
import dubbo.mini.common.URL;
import dubbo.mini.remote.*;

import java.net.InetSocketAddress;

/**
 * @author Administrator
 */
public abstract class AbstractEndpoint extends AbstractPeer implements ChannelEventHandler {


    private int timeout;

    private int connectTimeout;

    public AbstractEndpoint(URL url, ChannelEventHandler handler) {
        super(url, handler);

        this.timeout = url.getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        this.connectTimeout = url.getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT);
    }

    public int getTimeout() {
        return timeout;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public ChannelEventHandler getChannelHandler() {
        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public void send(Object message) throws RemotingException {

    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {

    }

}
