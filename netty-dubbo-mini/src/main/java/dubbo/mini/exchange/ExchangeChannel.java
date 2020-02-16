package dubbo.mini.exchange;

import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.support.ResponseFuture;

public interface ExchangeChannel extends NetChannel {

  ResponseFuture request(Object request) throws RemotingException;

  ResponseFuture request(Object request, int timeout) throws RemotingException;

  ExchangeHandler getExchangeHandler();

  @Override
  void close(int timeout);
}