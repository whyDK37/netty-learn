package dubbo.mini.remote;

import dubbo.mini.util.Assert;

public abstract class AbstractChannelHandlerDelegate implements ChannelHandlerDelegate {

  protected ChannelEventHandler handler;

  protected AbstractChannelHandlerDelegate(ChannelEventHandler handler) {
    Assert.notNull(handler, "handler == null");
    this.handler = handler;
  }

  @Override
  public ChannelEventHandler getHandler() {
    if (handler instanceof ChannelHandlerDelegate) {
      return ((ChannelHandlerDelegate) handler).getHandler();
    }
    return handler;
  }

  @Override
  public void connected(NetChannel channel) throws RemotingException {
    handler.connected(channel);
  }

  @Override
  public void disconnected(NetChannel channel) throws RemotingException {
    handler.disconnected(channel);
  }

  @Override
  public void sent(NetChannel channel, Object message) throws RemotingException {
    handler.sent(channel, message);
  }

  @Override
  public void received(NetChannel channel, Object message) throws RemotingException {
    handler.received(channel, message);
  }

  @Override
  public void caught(NetChannel channel, Throwable exception) throws RemotingException {
    handler.caught(channel, exception);
  }
}
