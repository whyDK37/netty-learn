package dubbo.mini.remote;

public interface Client extends Endpoint, NetChannel {

    /**
     * reconnect.
     */
    void reconnect() throws RemotingException;

}
