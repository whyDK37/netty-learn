package dubbo.mini.exchange;

import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.support.ResponseFuture;

public interface ExchangeChannel extends NetChannel {

    /**
     * send request.
     *
     * @param request
     * @return response future
     * @throws RemotingException
     */
    ResponseFuture request(Object request) throws RemotingException;

    /**
     * send request.
     *
     * @param request
     * @param timeout
     * @return response future
     * @throws RemotingException
     */
    ResponseFuture request(Object request, int timeout) throws RemotingException;

    /**
     * get message handler.
     *
     * @return message handler
     */
    ExchangeHandler getExchangeHandler();

    /**
     * graceful close.
     *
     * @param timeout
     */
    @Override
    void close(int timeout);
}