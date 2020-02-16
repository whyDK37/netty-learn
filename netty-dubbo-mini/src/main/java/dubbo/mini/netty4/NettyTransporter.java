package dubbo.mini.netty4;

import dubbo.mini.common.NetURL;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.Client;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.remote.Server;
import dubbo.mini.remote.Transporter;

public class NettyTransporter implements Transporter {

  public static final String NAME = "netty";

  @Override
  public Server bind(NetURL url, ChannelEventHandler listener) throws RemotingException {
    return new NettyServer(url, listener);
  }

  @Override
  public Client connect(NetURL url, ChannelEventHandler listener) throws RemotingException {
    return new NettyClient(url, listener);
  }

}