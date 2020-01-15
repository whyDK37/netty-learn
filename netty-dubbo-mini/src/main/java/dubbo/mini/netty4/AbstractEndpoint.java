package dubbo.mini.netty4;

import dubbo.mini.codec.Codec;
import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.support.ExtensionLoader;

import java.net.InetSocketAddress;

/**
 * @author Administrator
 */
public abstract class AbstractEndpoint extends AbstractPeer implements ChannelEventHandler {


    private int timeout;

    private int connectTimeout;

    private Codec codec;

    public AbstractEndpoint(NetURL url, ChannelEventHandler handler) {
        super(url, handler);
        this.codec = getChannelCodec(url);
        this.timeout = url.getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        this.connectTimeout = url.getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT);
    }


    protected static Codec getChannelCodec(NetURL url) {
        String codecName = url.getParameter(Constants.CODEC_KEY, "telnet");
        return ExtensionLoader.getExtensionLoader(Codec.class).getExtension(codecName);
    }

    public int getTimeout() {
        return timeout;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    @Override
    public NetURL getUrl() {
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


    protected Codec getCodec() {
        return codec;
    }
}
