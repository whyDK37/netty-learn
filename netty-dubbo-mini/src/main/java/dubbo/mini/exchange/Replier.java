package dubbo.mini.exchange;

import dubbo.mini.exchange.ExchangeChannel;
import dubbo.mini.remote.RemotingException;

public interface Replier<T> {

    /**
     * reply.
     * 和 ExchangeHandler 最大的不同是，使用的是泛型 T，而不是固定的 Request 。
     * @param channel
     * @param request
     * @return response
     * @throws RemotingException
     */
    Object reply(ExchangeChannel channel, T request) throws RemotingException;

}