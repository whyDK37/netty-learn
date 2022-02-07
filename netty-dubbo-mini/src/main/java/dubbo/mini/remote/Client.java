package dubbo.mini.remote;

public interface Client extends Endpoint, NetChannel, IdleSensible, Resetable {

  void reconnect() throws RemotingException;

}
