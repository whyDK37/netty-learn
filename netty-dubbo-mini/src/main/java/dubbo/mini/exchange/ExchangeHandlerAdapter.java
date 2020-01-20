package dubbo.mini.exchange;

import dubbo.mini.remote.RemotingException;
import dubbo.mini.remote.TelnetHandlerAdapter;

import java.util.concurrent.CompletableFuture;

public abstract class ExchangeHandlerAdapter extends TelnetHandlerAdapter implements ExchangeHandler {

    @Override
    public CompletableFuture<Object> reply(ExchangeChannel channel, Object msg) throws RemotingException {
        return null;
    }

}