package dubbo.mini.remote;

import dubbo.mini.support.SPI;

@SPI
public interface TelnetHandler {

  String telnet(NetChannel channel, String message) throws RemotingException;

}