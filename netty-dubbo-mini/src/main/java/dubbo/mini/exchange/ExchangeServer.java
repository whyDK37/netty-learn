package dubbo.mini.exchange;

import dubbo.mini.remote.Server;

import java.net.InetSocketAddress;
import java.util.Collection;

public interface ExchangeServer extends Server {

    /**
     * get channels.
     *
     * @return channels
     */
    Collection<ExchangeChannel> getExchangeChannels();

    /**
     * get channel.
     *
     * @param remoteAddress
     * @return channel
     */
    ExchangeChannel getExchangeChannel(InetSocketAddress remoteAddress);

}