package dubbo.mini.exchange;

import dubbo.mini.remote.Server;
import java.net.InetSocketAddress;
import java.util.Collection;

public interface ExchangeServer extends Server {

  Collection<ExchangeChannel> getExchangeChannels();

  ExchangeChannel getExchangeChannel(InetSocketAddress remoteAddress);

}