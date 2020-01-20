package dubbo.mini.exchange;

import dubbo.mini.exchange.ExchangeChannel;
import dubbo.mini.remote.RemotingException;

public interface Replier<T> {

    Object reply(ExchangeChannel channel, T request) throws RemotingException;

}