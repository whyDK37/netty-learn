package dubbo.mini.exchange;

import dubbo.mini.common.NetURL;
import dubbo.mini.remote.RemotingException;

/**
 * @author why
 */
public interface Exchanger {

    ExchangeServer bind(NetURL url, ExchangeHandler handler) throws RemotingException;


    ExchangeClient connect(NetURL url, ExchangeHandler handler) throws RemotingException;

}
