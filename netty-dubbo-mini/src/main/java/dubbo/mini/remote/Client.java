package dubbo.mini.remote;

public interface Client extends Endpoint, NetChannel {

    void reconnect() throws RemotingException;

}
