package dubbo.mini.remote;

import dubbo.mini.common.NetURL;
import java.net.InetSocketAddress;

public interface Endpoint {


  NetURL getUrl();

  ChannelEventHandler getChannelHandler();

  InetSocketAddress getLocalAddress();

  void send(Object message) throws RemotingException;

  void send(Object message, boolean sent) throws RemotingException;

  void close();

  void close(int timeout);

  void startClose();

  boolean isClosed();

}