package dubbo.mini.exchange;

import dubbo.mini.common.NetURL;
import dubbo.mini.exchange.header.HeaderExchanger;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.support.SPI;

/**
 * @author why
 */
@SPI(HeaderExchanger.NAME)
public interface Exchanger {

  ExchangeServer bind(NetURL url, ExchangeHandler handler) throws RemotingException;


  ExchangeClient connect(NetURL url, ExchangeHandler handler) throws RemotingException;

}
