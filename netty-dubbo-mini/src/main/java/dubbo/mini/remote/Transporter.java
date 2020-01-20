package dubbo.mini.remote;

import dubbo.mini.common.NetURL;
import dubbo.mini.support.SPI;

@SPI("netty")
public interface Transporter {

    /**
     * Bind a server.
     *
     * @param url     server url
     * @param handler
     * @return server
     * @throws RemotingException
     */
    Server bind(NetURL url, ChannelEventHandler handler) throws RemotingException;

    /**
     * Connect to a server.
     *
     * @param url     server url
     * @param handler
     * @return client
     * @throws RemotingException
     */
    Client connect(NetURL url, ChannelEventHandler handler) throws RemotingException;

}