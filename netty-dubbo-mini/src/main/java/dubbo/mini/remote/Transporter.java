package dubbo.mini.remote;

import dubbo.mini.common.NetURL;
import dubbo.mini.support.SPI;

@SPI("netty")
public interface Transporter {

  Server bind(NetURL url, ChannelEventHandler handler) throws RemotingException;

  Client connect(NetURL url, ChannelEventHandler handler) throws RemotingException;

}