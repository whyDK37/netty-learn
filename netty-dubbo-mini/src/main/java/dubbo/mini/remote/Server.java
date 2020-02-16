package dubbo.mini.remote;

import java.net.InetSocketAddress;
import java.util.Collection;

public interface Server extends Endpoint, IdleSensible, Resetable {

  boolean isBound();

  Collection<NetChannel> getChannels();

  NetChannel getChannel(InetSocketAddress remoteAddress);

}