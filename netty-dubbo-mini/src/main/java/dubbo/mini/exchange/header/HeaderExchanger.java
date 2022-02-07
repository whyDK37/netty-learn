package dubbo.mini.exchange.header;

import dubbo.mini.common.NetURL;
import dubbo.mini.exchange.ExchangeClient;
import dubbo.mini.exchange.ExchangeHandler;
import dubbo.mini.exchange.ExchangeServer;
import dubbo.mini.exchange.Exchanger;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.transport.DecodeHandler;
import dubbo.mini.transport.Transporters;

public class HeaderExchanger implements Exchanger {

  public static final String NAME = "header";

  @Override
  public ExchangeClient connect(NetURL url, ExchangeHandler handler) throws RemotingException {
    return new HeaderExchangeClient(
        Transporters.connect(url, new DecodeHandler(new HeaderExchangeHandler(handler))), true);
  }

  @Override
  public ExchangeServer bind(NetURL url, ExchangeHandler handler) throws RemotingException {
    return new HeaderExchangeServer(
        Transporters.bind(url, new DecodeHandler(new HeaderExchangeHandler(handler))));
  }

}