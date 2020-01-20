package dubbo.mini.remote;

import dubbo.mini.support.SPI;

@SPI
public interface TelnetHandler {

    /**
     * telnet.
     *
     * @param channel
     * @param message
     */
    String telnet(NetChannel channel, String message) throws RemotingException;

}