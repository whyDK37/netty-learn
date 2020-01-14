package dubbo.mini.remote;

public interface ChannelEventHandler {

    /**
     * on channel connected.
     *
     * @param channel channel.
     */
    void connected(NetChannel channel) throws RemotingException;

    /**
     * on channel disconnected.
     *
     * @param channel channel.
     */
    void disconnected(NetChannel channel) throws RemotingException;

    /**
     * on message sent.
     *
     * @param channel channel.
     * @param message message.
     */
    void sent(NetChannel channel, Object message) throws RemotingException;

    /**
     * on message received.
     *
     * @param channel channel.
     * @param message message.
     */
    void received(NetChannel channel, Object message) throws RemotingException;

    /**
     * on exception caught.
     *
     * @param channel   channel.
     * @param exception exception.
     */
    void caught(NetChannel channel, Throwable exception) throws RemotingException;
}
