package dubbo.mini.remote;

import dubbo.mini.common.NetURL;

import java.net.InetSocketAddress;

public interface Endpoint {

    /**
     * get url.
     *
     * @return url
     */
    NetURL getUrl();

    /**
     * get channel handler.
     *
     * @return channel handler
     */
    ChannelEventHandler getChannelHandler();

    /**
     * get local address.
     *
     * @return local address.
     */
    InetSocketAddress getLocalAddress();

    /**
     * send message.
     *
     * @param message
     * @throws RemotingException
     */
    void send(Object message) throws RemotingException;

    /**
     * send message.
     *
     * @param message
     * @param sent    already sent to socket?
     */
    void send(Object message, boolean sent) throws RemotingException;

    /**
     * close the channel.
     */
    void close();

    /**
     * Graceful close the channel.
     */
    void close(int timeout);

    void startClose();

    /**
     * is closed.
     *
     * @return closed
     */
    boolean isClosed();

}