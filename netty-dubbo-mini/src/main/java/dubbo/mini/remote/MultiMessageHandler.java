package dubbo.mini.remote;

import dubbo.mini.remote.support.MultiMessage;

public class MultiMessageHandler extends AbstractChannelHandlerDelegate {

  public MultiMessageHandler(ChannelEventHandler handler) {
    super(handler);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void received(NetChannel channel, Object message) throws RemotingException {
    if (message instanceof MultiMessage) {
      MultiMessage list = (MultiMessage) message;
      for (Object obj : list) {
        handler.received(channel, obj);
      }
    } else {
      handler.received(channel, message);
    }
  }
}