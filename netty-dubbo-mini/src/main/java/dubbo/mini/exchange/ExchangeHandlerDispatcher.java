package dubbo.mini.exchange;

import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.ChannelHandlerDispatcher;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import java.util.concurrent.CompletableFuture;

public class ExchangeHandlerDispatcher implements ExchangeHandler {


  /**
   * 回复者调度器
   */
  private final ReplierDispatcher replierDispatcher;

  /**
   * 通道处理器集合
   */
  private final ChannelHandlerDispatcher handlerDispatcher;

  public ExchangeHandlerDispatcher(Replier<?> replier, ChannelEventHandler handlers) {
    replierDispatcher = new ReplierDispatcher(replier);
    handlerDispatcher = new ChannelHandlerDispatcher(handlers);
  }


  public ExchangeHandlerDispatcher addChannelHandler(ChannelEventHandler handler) {
    handlerDispatcher.addChannelHandler(handler);
    return this;
  }


  public ExchangeHandlerDispatcher removeChannelHandler(ChannelEventHandler handler) {
    handlerDispatcher.removeChannelHandler(handler);
    return this;
  }

  @Override
  public CompletableFuture<Object> reply(ExchangeChannel channel, Object request)
      throws RemotingException {
    return CompletableFuture.completedFuture(((Replier) replierDispatcher).reply(channel, request));
  }

  @Override
  public void connected(NetChannel channel) throws RemotingException {
    handlerDispatcher.connected(channel);
  }

  @Override
  public void disconnected(NetChannel channel) throws RemotingException {
    handlerDispatcher.disconnected(channel);
  }

  @Override
  public void sent(NetChannel channel, Object message) throws RemotingException {
    handlerDispatcher.sent(channel, message);

  }

  @Override
  public void received(NetChannel channel, Object message) throws RemotingException {
    handlerDispatcher.received(channel, message);

  }

  @Override
  public void caught(NetChannel channel, Throwable exception) throws RemotingException {
    handlerDispatcher.caught(channel, exception);
  }
}
