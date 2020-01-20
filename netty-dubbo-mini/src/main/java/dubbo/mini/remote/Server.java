package dubbo.mini.remote;

import java.net.InetSocketAddress;
import java.util.Collection;

public interface Server extends Endpoint {

    /**
     * is bound.
     *
     * @return bound
     */
    boolean isBound();

    /**
     * get channels.
     *
     * @return channels
     */
    Collection<NetChannel> getChannels();

    /**
     * get channel.
     *
     * @param remoteAddress
     * @return channel
     */
    NetChannel getChannel(InetSocketAddress remoteAddress);

}