package dubbo.mini.remote;

public class ChannelHandlerAdapter implements ChannelEventHandler {

  @Override
  public void connected(NetChannel channel) throws RemotingException {
  }

  @Override
  public void disconnected(NetChannel channel) throws RemotingException {
  }

  @Override
  public void sent(NetChannel channel, Object message) throws RemotingException {
  }

  @Override
  public void received(NetChannel channel, Object message) throws RemotingException {
  }

  @Override
  public void caught(NetChannel channel, Throwable exception) throws RemotingException {
  }

}
