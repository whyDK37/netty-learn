package dubbo.mini.exchange;

import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.RemotingException;
import java.util.concurrent.CompletableFuture;

public interface ExchangeHandler extends ChannelEventHandler {

  CompletableFuture<Object> reply(ExchangeChannel channel, Object request) throws RemotingException;

}