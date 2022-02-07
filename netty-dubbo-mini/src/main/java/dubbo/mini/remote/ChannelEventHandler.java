package dubbo.mini.remote;

public interface ChannelEventHandler {

  void connected(NetChannel channel) throws RemotingException;

  void disconnected(NetChannel channel) throws RemotingException;


  void sent(NetChannel channel, Object message) throws RemotingException;


  void received(NetChannel channel, Object message) throws RemotingException;


  void caught(NetChannel channel, Throwable exception) throws RemotingException;
}
